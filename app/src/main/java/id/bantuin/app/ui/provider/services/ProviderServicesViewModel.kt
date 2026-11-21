package id.bantuin.app.ui.provider.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderServicesUiState(
    val providerId: String = "p01",
    val services: List<ServiceItem> = emptyList(),
    val categories: List<ServiceCategory> = emptyList(),
    val isUpsertDialogOpen: Boolean = false,
    val editingService: ServiceItem? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProviderServicesViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderServicesUiState())
    val uiState: StateFlow<ProviderServicesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(providerId: String = "p01") {
        viewModelScope.launch {
            _uiState.update { it.copy(providerId = providerId, isLoading = true, errorMessage = null) }
            val servicesRes = catalogRepository.servicesOf(providerId)
            val categoriesRes = catalogRepository.categories()

            val services = if (servicesRes is AppResult.Success) servicesRes.data else emptyList()
            val categories = if (categoriesRes is AppResult.Success) categoriesRes.data else FakeData.categories

            _uiState.update {
                it.copy(
                    services = services,
                    categories = categories,
                    isLoading = false
                )
            }
        }
    }

    fun openAddDialog() {
        _uiState.update {
            it.copy(
                isUpsertDialogOpen = true,
                editingService = null,
                errorMessage = null
            )
        }
    }

    fun openEditDialog(service: ServiceItem) {
        _uiState.update {
            it.copy(
                isUpsertDialogOpen = true,
                editingService = service,
                errorMessage = null
            )
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                isUpsertDialogOpen = false,
                editingService = null,
                errorMessage = null
            )
        }
    }

    fun saveService(service: ServiceItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val res = catalogRepository.upsertService(service)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isUpsertDialogOpen = false,
                            editingService = null
                        )
                    }
                    loadData(_uiState.value.providerId)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            when (val res = catalogRepository.deleteService(serviceId)) {
                is AppResult.Success -> {
                    loadData(_uiState.value.providerId)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
            }
        }
    }
}
