package id.bantuin.app.ui.customer.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
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

enum class ProviderSortOption(val label: String, val subtitle: String) {
    RECOMMENDED("Rekomendasi Terbaik", "Gabungan jarak, rating, dan rasio selesai"),
    NEAREST("Jarak Terdekat", "Mitra paling dekat dari lokasi Anda"),
    HIGHEST_RATING("Rating Tertinggi", "Penilaian pelanggan tertinggi"),
    LOWEST_PRICE("Tarif Terendah", "Biaya dasar mulai paling terjangkau"),
    MOST_REVIEWS("Ulasan Terbanyak", "Paling sering dipesan dan diulas")
}

data class RecommendationUiState(
    val categoryId: Int = 1,
    val category: ServiceCategory = ServiceCategory(1, "Kategori Jasa", "build", 60, "Layanan teknisi dan bantuan rumah"),
    val customerLocation: GeoPoint = BantuinConfig.MALANG_CENTER,
    val maxDistanceFilter: Double = 10.0,
    val minRatingFilter: Double = 0.0,
    val onlyTrusted: Boolean = false,
    val sortOption: ProviderSortOption = ProviderSortOption.RECOMMENDED,
    val rawCandidates: List<ProviderCandidate> = emptyList(),
    val matchResult: MatchResult = MatchResult(emptyList(), emptyList()),
    val filteredList: List<RankedProvider> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val activeFilterCount: Int
        get() = listOf(
            maxDistanceFilter < 10.0,
            minRatingFilter > 0.0,
            onlyTrusted
        ).count { it }

    val hasActiveFilterOrSort: Boolean
        get() = activeFilterCount > 0 || sortOption != ProviderSortOption.RECOMMENDED
}

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
                            onlyTrusted = _uiState.value.onlyTrusted,
                            sortOption = _uiState.value.sortOption
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

    fun setSortOption(sortOption: ProviderSortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        recomputeFiltered()
    }

    fun applyFiltersAndSort(
        maxDistance: Double,
        minRating: Double,
        onlyTrusted: Boolean,
        sortOption: ProviderSortOption
    ) {
        _uiState.update {
            it.copy(
                maxDistanceFilter = maxDistance,
                minRatingFilter = minRating,
                onlyTrusted = onlyTrusted,
                sortOption = sortOption
            )
        }
        recomputeFiltered()
    }

    fun resetFilters() {
        applyFiltersAndSort(
            maxDistance = 10.0,
            minRating = 0.0,
            onlyTrusted = false,
            sortOption = ProviderSortOption.RECOMMENDED
        )
    }

    fun previewMatchCount(
        maxDistance: Double,
        minRating: Double,
        onlyTrusted: Boolean
    ): Int {
        return filterRanked(_uiState.value.matchResult.ranked, maxDistance, minRating, onlyTrusted).size
    }

    private fun recomputeRankings(
        category: ServiceCategory,
        candidates: List<ProviderCandidate>,
        location: GeoPoint,
        maxDistance: Double,
        minRating: Double,
        onlyTrusted: Boolean,
        sortOption: ProviderSortOption
    ) {
        val matchResult = matchmakingUseCase(location, candidates)
        val filteredAndSorted = sortRanked(
            filterRanked(matchResult.ranked, maxDistance, minRating, onlyTrusted),
            sortOption
        )
        _uiState.update {
            it.copy(
                category = category,
                rawCandidates = candidates,
                matchResult = matchResult,
                filteredList = filteredAndSorted,
                isLoading = false
            )
        }
    }

    private fun recomputeFiltered() {
        val state = _uiState.value
        val filteredAndSorted = sortRanked(
            filterRanked(
                state.matchResult.ranked,
                state.maxDistanceFilter,
                state.minRatingFilter,
                state.onlyTrusted
            ),
            state.sortOption
        )
        _uiState.update { it.copy(filteredList = filteredAndSorted) }
    }

    private fun filterRanked(
        ranked: List<RankedProvider>,
        maxDistance: Double,
        minRating: Double,
        onlyTrusted: Boolean
    ): List<RankedProvider> {
        return ranked.filter { item ->
            val distPass = item.distanceKm <= maxDistance
            val ratingPass = (item.candidate.ratingAvg ?: 0.0) >= minRating
            val trustedPass = !onlyTrusted || item.candidate.badge == ProviderBadge.TRUSTED
            distPass && ratingPass && trustedPass
        }
    }

    private fun sortRanked(
        items: List<RankedProvider>,
        sortOption: ProviderSortOption
    ): List<RankedProvider> {
        return when (sortOption) {
            ProviderSortOption.RECOMMENDED -> items
            ProviderSortOption.NEAREST -> items.sortedWith(
                compareBy<RankedProvider> { it.distanceKm }
                    .thenByDescending { it.matchScore }
            )
            ProviderSortOption.HIGHEST_RATING -> items.sortedWith(
                compareByDescending<RankedProvider> { it.candidate.ratingAvg ?: 0.0 }
                    .thenByDescending { it.candidate.reviewCount }
                    .thenBy { it.distanceKm }
            )
            ProviderSortOption.LOWEST_PRICE -> items.sortedWith(
                compareBy<RankedProvider> { it.candidate.minBaseFare ?: Int.MAX_VALUE }
                    .thenBy { it.distanceKm }
            )
            ProviderSortOption.MOST_REVIEWS -> items.sortedWith(
                compareByDescending<RankedProvider> { it.candidate.reviewCount }
                    .thenByDescending { it.candidate.ratingAvg ?: 0.0 }
            )
        }
    }
}
