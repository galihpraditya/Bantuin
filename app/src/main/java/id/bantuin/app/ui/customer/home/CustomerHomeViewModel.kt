package id.bantuin.app.ui.customer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerHomeUiState(
    val user: UserProfile? = null,
    val customerAddress: String = "Jl. Merdeka Barat, Alun-alun Malang",
    val customerLocation: GeoPoint = FakeData.MALANG_CENTER,
    val categories: List<ServiceCategory> = emptyList(),
    val showLocationDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CustomerHomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerHomeUiState())
    val uiState: StateFlow<CustomerHomeUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        loadCategories()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.currentUser().collect { userProfile ->
                _uiState.update { it.copy(user = userProfile) }
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = catalogRepository.categories()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(categories = result.data, isLoading = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(categories = FakeData.categories, isLoading = false) }
                }
            }
        }
    }

    fun updateLocation(address: String, location: GeoPoint) {
        _uiState.update {
            it.copy(
                customerAddress = address,
                customerLocation = location,
                showLocationDialog = false
            )
        }
    }

    fun setShowLocationDialog(show: Boolean) {
        _uiState.update { it.copy(showLocationDialog = show) }
    }
}
