package id.bantuin.app.ui.provider.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderProfileEditUiState(
    val providerId: String = "p01",
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
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderProfileEditUiState())
    val uiState: StateFlow<ProviderProfileEditUiState> = _uiState.asStateFlow()

    fun loadProfile(providerId: String = "p01") {
        viewModelScope.launch {
            _uiState.update { it.copy(providerId = providerId, isLoading = true, errorMessage = null) }
            when (val res = catalogRepository.candidates(2)) { // AC category where p01 resides
                is AppResult.Success -> {
                    val p = res.data.find { it.providerId == providerId } ?: res.data.firstOrNull()
                    if (p != null) {
                        _uiState.update {
                            it.copy(
                                name = p.name,
                                bio = p.bio,
                                phone = "081234567890",
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
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, isSaved = false) }
    fun onBioChange(bio: String) = _uiState.update { it.copy(bio = bio, isSaved = false) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phone = phone, isSaved = false) }
    fun onAddressChange(address: String) = _uiState.update { it.copy(address = address, isSaved = false) }
    fun onLocationChange(location: GeoPoint) = _uiState.update { it.copy(location = location, isSaved = false) }

    fun saveProfile() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama lengkap tidak boleh kosong") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val res = catalogRepository.updateProviderProfile(
                providerId = state.providerId,
                name = state.name,
                bio = state.bio,
                location = state.location,
                address = state.address,
                phone = state.phone
            )
            when (res) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.message) }
                }
            }
        }
    }
}
