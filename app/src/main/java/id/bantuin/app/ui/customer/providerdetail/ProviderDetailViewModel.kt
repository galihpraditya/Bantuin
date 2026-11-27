package id.bantuin.app.ui.customer.providerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.ReviewRepository
import id.bantuin.app.domain.usecase.bookmark.BookmarkUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val errorMessage: String? = null,
    val isBookmarked: Boolean = false
)

class ProviderDetailViewModel(
    private val catalogRepository: CatalogRepository,
    private val reviewRepository: ReviewRepository,
    private val bookmarkUseCase: BookmarkUseCase = BookmarkUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderDetailUiState())
    val uiState: StateFlow<ProviderDetailUiState> = _uiState.asStateFlow()

    fun loadProviderDetail(providerId: String, initialProvider: ProviderCandidate? = null) {
        if (initialProvider != null) {
            _uiState.update { it.copy(provider = initialProvider) }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val profileDeferred = if (initialProvider == null) {
                    async { catalogRepository.providerProfile(providerId) }
                } else null
                val servicesDeferred = async { catalogRepository.servicesOf(providerId) }
                val reviewsDeferred = async { reviewRepository.reviewsOf(providerId) }

                val profileRes = profileDeferred?.await()
                val servicesRes = servicesDeferred.await()
                val reviewsRes = reviewsDeferred.await()

                val resolvedProvider = when {
                    initialProvider != null -> initialProvider
                    profileRes is AppResult.Success -> profileRes.data
                    else -> _uiState.value.provider
                }

                val errorMsg = when {
                    profileRes is AppResult.Error && resolvedProvider == null -> profileRes.message
                    servicesRes is AppResult.Error -> servicesRes.message
                    resolvedProvider == null -> "Profil mitra tidak ditemukan."
                    else -> null
                }

                val services = if (servicesRes is AppResult.Success) {
                    servicesRes.data.filter { it.isActive }
                } else {
                    emptyList()
                }
                val reviews = if (reviewsRes is AppResult.Success) {
                    reviewsRes.data
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        provider = resolvedProvider,
                        services = services,
                        reviews = reviews,
                        isLoading = false,
                        errorMessage = errorMsg,
                        isBookmarked = bookmarkUseCase.isBookmarked("user_demo", providerId)
                    )
                }
            }
        }
    }

    fun toggleBookmark(categoryId: Int = 1, customerId: String = "user_demo") {
        val provider = _uiState.value.provider ?: return
        val nowBookmarked = bookmarkUseCase.toggle(customerId, provider, categoryId)
        _uiState.update { it.copy(isBookmarked = nowBookmarked) }
    }
}
