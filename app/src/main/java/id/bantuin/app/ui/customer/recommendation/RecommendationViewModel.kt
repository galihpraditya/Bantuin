package id.bantuin.app.ui.customer.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.MatchResult
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecommendationUiState(
    val categoryId: Int = 1,
    val category: ServiceCategory = ServiceCategory(1, "Kategori Jasa", "build", 60, "Layanan teknisi dan bantuan rumah"),
    val customerLocation: GeoPoint = FakeData.MALANG_CENTER,
    val maxDistanceFilter: Double = 10.0,
    val minRatingFilter: Double = 0.0,
    val onlyTrusted: Boolean = false,
    val rawCandidates: List<ProviderCandidate> = emptyList(),
    val matchResult: MatchResult = MatchResult(emptyList(), emptyList()),
    val filteredList: List<RankedProvider> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class RecommendationViewModel(
    private val catalogRepository: CatalogRepository,
    private val matchmakingUseCase: MatchmakingUseCase = MatchmakingUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    private var loadedCategoryId: Int? = null
    private var loadedLocation: GeoPoint? = null

    fun initialize(categoryId: Int, customerLocation: GeoPoint) {
        if (loadedCategoryId == categoryId &&
            loadedLocation == customerLocation &&
            _uiState.value.rawCandidates.isNotEmpty()
        ) {
            return
        }
        _uiState.update {
            it.copy(
                categoryId = categoryId,
                customerLocation = customerLocation
            )
        }
        loadCandidates(categoryId, customerLocation)
    }

    fun loadCandidates(
        categoryId: Int = _uiState.value.categoryId,
        customerLocation: GeoPoint = _uiState.value.customerLocation
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val catsDeferred = async { catalogRepository.categories() }
                val candDeferred = async { catalogRepository.candidates(categoryId) }

                val catsRes = catsDeferred.await()
                val resolvedCategory = (catsRes as? AppResult.Success)?.data
                    ?.find { it.id == categoryId }
                    ?: _uiState.value.category

                when (val res = candDeferred.await()) {
                    is AppResult.Success -> {
                        loadedCategoryId = categoryId
                        loadedLocation = customerLocation
                        recomputeRankings(
                            category = resolvedCategory,
                            candidates = res.data,
                            location = customerLocation,
                            maxDistance = _uiState.value.maxDistanceFilter,
                            minRating = _uiState.value.minRatingFilter,
                            onlyTrusted = _uiState.value.onlyTrusted
                        )
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                category = resolvedCategory,
                                isLoading = false,
                                errorMessage = res.message
                            )
                        }
                    }
                    is AppResult.Loading -> {}
                }
            }
        }
    }

    fun setMaxDistanceFilter(maxDist: Double) {
        _uiState.update { it.copy(maxDistanceFilter = maxDist) }
        recomputeFiltered()
    }

    fun setMinRatingFilter(minRating: Double) {
        _uiState.update { it.copy(minRatingFilter = minRating) }
        recomputeFiltered()
    }

    fun setOnlyTrusted(onlyTrusted: Boolean) {
        _uiState.update { it.copy(onlyTrusted = onlyTrusted) }
        recomputeFiltered()
    }

    private fun recomputeRankings(
        category: ServiceCategory,
        candidates: List<ProviderCandidate>,
        location: GeoPoint,
        maxDistance: Double,
        minRating: Double,
        onlyTrusted: Boolean
    ) {
        val matchResult = matchmakingUseCase(location, candidates)
        val filtered = matchResult.ranked.filter { ranked ->
            val distPass = ranked.distanceKm <= maxDistance
            val ratingPass = (ranked.candidate.ratingAvg ?: 0.0) >= minRating
            val trustedPass = !onlyTrusted || ranked.candidate.badge == ProviderBadge.TRUSTED
            distPass && ratingPass && trustedPass
        }
        _uiState.update {
            it.copy(
                category = category,
                rawCandidates = candidates,
                matchResult = matchResult,
                filteredList = filtered,
                isLoading = false
            )
        }
    }

    private fun recomputeFiltered() {
        val state = _uiState.value
        val filtered = state.matchResult.ranked.filter { ranked ->
            val distPass = ranked.distanceKm <= state.maxDistanceFilter
            val ratingPass = (ranked.candidate.ratingAvg ?: 0.0) >= state.minRatingFilter
            val trustedPass = !state.onlyTrusted || ranked.candidate.badge == ProviderBadge.TRUSTED
            distPass && ratingPass && trustedPass
        }
        _uiState.update { it.copy(filteredList = filtered) }
    }
}
