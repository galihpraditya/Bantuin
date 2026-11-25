package id.bantuin.app.ui.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
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

data class CustomerProfileUiState(
    val user: UserProfile? = null,
    val activeRole: String = "customer",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CustomerProfileViewModel(
    private val authRepository: AuthRepository,
    private val themeStore: ThemePreferenceStore = InMemoryThemeStore()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CustomerProfileUiState(themeMode = themeStore.themeModeValue)
    )
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
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
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeStore.setThemeMode(mode)
        }
    }

    /**
     * Beralih peran hanya diperbolehkan bila akun memang memiliki role tersebut.
     * Untuk mode mitra, akun tanpa role "provider" diarahkan ke halaman login
     * (sesi dibersihkan) agar tidak menampilkan profil mitra demo orang lain.
     */
    fun switchRole(
        newRole: String,
        onAllowed: () -> Unit,
        onUnauthorized: () -> Unit
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            if (newRole == "provider" && user?.roles?.contains("provider") != true) {
                authRepository.signOut()
                onUnauthorized()
                return@launch
            }
            authRepository.setActiveRole(newRole)
            onAllowed()
        }
    }

    fun activateProviderMode(
        address: String,
        location: GeoPoint,
        bio: String,
        onSuccess: () -> Unit
    ) {
        if (address.isBlank() || bio.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Alamat usaha dan ringkasan keahlian wajib diisi.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = authRepository.enableProviderRole(address.trim(), location, bio.trim())) {
                is AppResult.Success -> {
                    authRepository.setActiveRole("provider")
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
