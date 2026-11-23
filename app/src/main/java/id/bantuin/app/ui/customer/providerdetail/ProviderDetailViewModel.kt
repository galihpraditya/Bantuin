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

            var resolvedProvider = initialProvider ?: _uiState.value.provider
            if (resolvedProvider == null) {
                val cats = catalogRepository.categories()
                if (cats is AppResult.Success) {
                    for (cat in cats.data) {
                        val candRes = catalogRepository.candidates(cat.id)
                        if (candRes is AppResult.Success) {
                            val found = candRes.data.find { it.providerId == providerId }
                            if (found != null) {
                                resolvedProvider = found
                                break
                            }
                        }
                    }
                }
                if (resolvedProvider == null) {
                    resolvedProvider = FakeData.providers.find { it.providerId == providerId }
                }
            }

            val services = when (servicesRes) {
                is AppResult.Success -> servicesRes.data.filter { it.isActive }
                is AppResult.Error -> FakeData.services.filter { it.providerId == providerId && it.isActive }
                is AppResult.Loading -> emptyList()
            }

            val reviews = when (reviewsRes) {
                is AppResult.Success -> reviewsRes.data
                is AppResult.Error -> FakeData.sampleReviews
                is AppResult.Loading -> emptyList()
            }

            val error = if (servicesRes is AppResult.Error && services.isEmpty()) {
                servicesRes.message
            } else null

            _uiState.update {
                it.copy(
                    provider = resolvedProvider,
                    services = services,
                    reviews = reviews,
                    isLoading = false,
                    errorMessage = error
                )
            }
        }
    }
}
