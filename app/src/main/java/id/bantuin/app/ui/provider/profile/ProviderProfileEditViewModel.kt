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
    val isDetectingGps: Boolean = false,
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
            when (val res = catalogRepository.providerProfile(resolvedId)) {
                is AppResult.Success -> {
                    val p = res.data
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
    fun setDetectingGps(detecting: Boolean) = _uiState.update { it.copy(isDetectingGps = detecting, errorMessage = null) }
    fun setGpsError(message: String?) = _uiState.update { it.copy(isDetectingGps = false, errorMessage = message) }

    fun saveProfile() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val normalizedPhone = state.phone.trim().replace(" ", "").replace("-", "")

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama lengkap tidak boleh kosong") }
            return
        }
        if (normalizedPhone.isNotBlank() && !INDONESIA_PHONE_REGEX.matches(normalizedPhone)) {
            _uiState.update {
                it.copy(errorMessage = "Nomor telepon Indonesia tidak valid (contoh: 081234567890)")
            }
            return
        }
        if (state.providerId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Akun mitra tidak ditemukan") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val res = catalogRepository.updateProviderProfile(
                providerId = state.providerId,
                name = trimmedName,
                bio = state.bio.trim(),
                location = state.location,
                address = state.address.trim(),
                phone = normalizedPhone
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

    companion object {
        private val INDONESIA_PHONE_REGEX = Regex("^(\\+62|62|0)8[1-9][0-9]{7,11}$")
    }
}
