package id.bantuin.app.ui.customer.providerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderDetailUiState(
    val provider: ProviderCandidate? = null,
    val services: List<ServiceItem> = emptyList(),
    val reviews: List<ReviewItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProviderDetailViewModel(
    private val catalogRepository: CatalogRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderDetailUiState())
    val uiState: StateFlow<ProviderDetailUiState> = _uiState.asStateFlow()

    fun loadProviderDetail(providerId: String, initialProvider: ProviderCandidate? = null) {
        if (initialProvider != null) {
            _uiState.update { it.copy(provider = initialProvider) }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val servicesRes = catalogRepository.servicesOf(providerId)
            val reviewsRes = reviewRepository.reviewsOf(providerId)

            val services = if (servicesRes is AppResult.Success) servicesRes.data else emptyList()
            val reviews = if (reviewsRes is AppResult.Success) reviewsRes.data else emptyList()

            _uiState.update {
                it.copy(
                    services = services,
                    reviews = reviews,
                    isLoading = false
                )
            }
        }
    }
}
