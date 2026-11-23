package id.bantuin.app.ui.provider.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderProfileEditUiState(
    val providerId: String = "",
    val name: String = "",
    val bio: String = "",
    val phone: String = "",
    val address: String = "",
    val location: GeoPoint = GeoPoint(-7.9650, 112.6320),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class ProviderProfileEditViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderProfileEditUiState())
    val uiState: StateFlow<ProviderProfileEditUiState> = _uiState.asStateFlow()

    /**
     * Profil mitra diambil dari akun yang login. Tidak lagi memakai "p01"/Budi
     * Santoso sebagai fallback.
     */
    fun loadProfile(providerId: String? = null) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() } ?: user?.id
            if (resolvedId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        name = user?.fullName ?: "",
                        phone = user?.phone ?: "",
                        isLoading = false
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    providerId = resolvedId,
                    name = it.name.ifBlank { user?.fullName ?: "" },
                    phone = it.phone.ifBlank { user?.phone ?: "" },
                    isLoading = true,
                    errorMessage = null
                )
            }
            when (val res = catalogRepository.candidates(2)) {
                is AppResult.Success -> {
                    val p = res.data.find { it.providerId == resolvedId }
                    if (p != null) {
                        _uiState.update {
                            it.copy(
                                name = p.name,
                                bio = p.bio,
                                phone = it.phone.ifBlank { user?.phone.orEmpty() },
                                address = p.address,
                                location = p.location,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, isSaved = false, errorMessage = null) }
    fun onBioChange(bio: String) = _uiState.update { it.copy(bio = bio, isSaved = false, errorMessage = null) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phone = phone, isSaved = false, errorMessage = null) }
    fun onAddressChange(address: String) = _uiState.update { it.copy(address = address, isSaved = false, errorMessage = null) }
    fun onLocationChange(location: GeoPoint) = _uiState.update { it.copy(location = location, isSaved = false, errorMessage = null) }
    fun clearErrorMessage() = _uiState.update { it.copy(errorMessage = null) }
    fun resetSaved() = _uiState.update { it.copy(isSaved = false) }

    fun saveProfile() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama lengkap tidak boleh kosong") }
            return
        }
        if (state.providerId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Akun mitra tidak ditemukan") }
            return
        }
        if (state.phone.isNotBlank() && !state.phone.matches(Regex("^[0-9+\\-\\s()]{8,20}$"))) {
            _uiState.update { it.copy(errorMessage = "Format nomor telepon tidak valid") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val res = catalogRepository.updateProviderProfile(
                providerId = state.providerId,
                name = state.name.trim(),
                bio = state.bio.trim(),
                location = state.location,
                address = state.address.trim(),
                phone = state.phone.trim()
            )
            when (res) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
