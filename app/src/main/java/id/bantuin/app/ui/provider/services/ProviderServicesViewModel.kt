package id.bantuin.app.ui.provider.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ProviderServicesUiState(
    val providerId: String = "",
    val services: List<ServiceItem> = emptyList(),
    val categories: List<ServiceCategory> = emptyList(),
    val isUpsertDialogOpen: Boolean = false,
    val editingService: ServiceItem? = null,
    val serviceToDelete: ServiceItem? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val formErrorMessage: String? = null,
    val errorMessage: String? = null
)

class ProviderServicesViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderServicesUiState())
    val uiState: StateFlow<ProviderServicesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(providerId: String? = null) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() } ?: user?.id
            if (resolvedId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Akun mitra tidak ditemukan") }
                return@launch
            }
            _uiState.update { it.copy(providerId = resolvedId, isLoading = true, errorMessage = null) }
            coroutineScope {
                val servicesDeferred = async { catalogRepository.servicesOf(resolvedId) }
                val categoriesDeferred = async { catalogRepository.categories() }

                val servicesRes = servicesDeferred.await()
                val categoriesRes = categoriesDeferred.await()

                val services = if (servicesRes is AppResult.Success) servicesRes.data else _uiState.value.services
                val categories = if (categoriesRes is AppResult.Success) categoriesRes.data else _uiState.value.categories
                val err = when {
                    servicesRes is AppResult.Error -> servicesRes.message
                    categoriesRes is AppResult.Error -> categoriesRes.message
                    else -> null
                }

                _uiState.update {
                    it.copy(
                        services = services,
                        categories = categories,
                        isLoading = false,
                        errorMessage = err
                    )
                }
            }
        }
    }

    fun openAddDialog() {
        _uiState.update {
            it.copy(
                isUpsertDialogOpen = true,
                editingService = null,
                formErrorMessage = null
            )
        }
    }

    fun openEditDialog(service: ServiceItem) {
        _uiState.update {
            it.copy(
                isUpsertDialogOpen = true,
                editingService = service,
                formErrorMessage = null
            )
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                isUpsertDialogOpen = false,
                editingService = null,
                formErrorMessage = null
            )
        }
    }

    fun clearFormError() {
        _uiState.update { it.copy(formErrorMessage = null) }
    }

    fun requestDelete(service: ServiceItem?) {
        _uiState.update { it.copy(serviceToDelete = service) }
    }

    fun toggleServiceActive(service: ServiceItem, active: Boolean) {
        val updated = service.copy(isActive = active)
        val optimistic = _uiState.value.services.map { if (it.id == service.id) updated else it }
        _uiState.update { it.copy(services = optimistic, errorMessage = null) }
        viewModelScope.launch {
            when (val res = catalogRepository.upsertService(updated)) {
                is AppResult.Success -> loadData(_uiState.value.providerId)
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                    loadData(_uiState.value.providerId)
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun saveServiceFromForm(
        existing: ServiceItem?,
        name: String,
        description: String,
        categoryId: Int,
        baseFareText: String,
        durationText: String
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(formErrorMessage = "Nama layanan wajib diisi") }
            return
        }
        val fare = baseFareText.toIntOrNull()
        if (fare == null || fare < 0) {
            _uiState.update { it.copy(formErrorMessage = "Tarif dasar tidak boleh negatif") }
            return
        }
        if (fare % 500 != 0) {
            _uiState.update {
                it.copy(formErrorMessage = "Tarif dasar harus kelipatan Rp 500 (contoh: Rp 0, Rp 75.000)")
            }
            return
        }
        val duration = durationText.toIntOrNull()
        if (duration == null || duration !in 15..720) {
            _uiState.update { it.copy(formErrorMessage = "Durasi harus antara 15 hingga 720 menit") }
            return
        }

        val item = ServiceItem(
            id = existing?.id ?: UUID.randomUUID().toString(),
            providerId = existing?.providerId ?: _uiState.value.providerId,
            categoryId = categoryId,
            name = trimmedName,
            description = description.trim().ifBlank { null },
            baseFare = fare,
            durationMin = duration,
            isActive = existing?.isActive ?: true
        )
        saveService(item)
    }

    fun saveService(service: ServiceItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, formErrorMessage = null) }
            when (val res = catalogRepository.upsertService(service)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isUpsertDialogOpen = false,
                            editingService = null,
                            formErrorMessage = null
                        )
                    }
                    loadData(_uiState.value.providerId)
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            formErrorMessage = res.message,
                            errorMessage = res.message
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            when (val res = catalogRepository.deleteService(serviceId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(serviceToDelete = null) }
                    loadData(_uiState.value.providerId)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(serviceToDelete = null, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
