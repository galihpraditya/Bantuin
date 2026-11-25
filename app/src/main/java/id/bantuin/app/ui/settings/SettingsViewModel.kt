package id.bantuin.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.settings.InMemorySettingsStore
import id.bantuin.app.core.settings.NotificationSettings
import id.bantuin.app.core.settings.SettingsPreferenceStore
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: UserProfile? = null,
    val activeRole: String = "customer",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val needsProviderRegistration: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val themeStore: ThemePreferenceStore = InMemoryThemeStore(),
    private val settingsStore: SettingsPreferenceStore = InMemorySettingsStore()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = themeStore.themeModeValue,
            notificationSettings = settingsStore.settingsValue
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            authRepository.activeRole().collect { role ->
                _uiState.update { it.copy(activeRole = role) }
            }
        }
        viewModelScope.launch {
            themeStore.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { it.copy(notificationSettings = settings) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeStore.setThemeMode(mode)
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(settings)
        }
    }

    /**
     * Beralih peran Pelanggan <-> Mitra Kerja tanpa memanggil signOut().
     * Jika pengguna belum memiliki role "provider", arahkan ke formulir aktivasi Mitra
     * sambil mempertahankan sesi login saat ini.
     */
    fun switchRoleWithoutSignOut(
        targetRole: String,
        onSwitched: () -> Unit,
        onNeedsProviderRegistration: () -> Unit
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            if (user == null) {
                _uiState.update { it.copy(errorMessage = "Sesi login tidak ditemukan.") }
                return@launch
            }
            if (targetRole == "provider" && "provider" !in user.roles) {
                _uiState.update { it.copy(needsProviderRegistration = true, errorMessage = null) }
                onNeedsProviderRegistration()
                return@launch
            }
            authRepository.setActiveRole(targetRole)
            _uiState.update { it.copy(needsProviderRegistration = false, errorMessage = null) }
            onSwitched()
        }
    }

    fun activateProviderRole(
        address: String,
        location: GeoPoint,
        bio: String,
        onSuccess: () -> Unit
    ) {
        val cleanAddress = address.trim()
        val cleanBio = bio.trim()
        if (cleanAddress.isBlank() || cleanBio.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Alamat pangkalan dan ringkasan keahlian wajib diisi.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = authRepository.enableProviderRole(cleanAddress, location, cleanBio)) {
                is AppResult.Success -> {
                    authRepository.setActiveRole("provider")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            needsProviderRegistration = false,
                            successMessage = "Akun Mitra Kerja berhasil diaktifkan."
                        )
                    }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
