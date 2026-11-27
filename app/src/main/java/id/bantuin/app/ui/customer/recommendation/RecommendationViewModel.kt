package id.bantuin.app.ui.customer.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookmarkedProvider
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.MatchResult
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RadiusExpansionState
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.usecase.bookmark.BookmarkUseCase
import id.bantuin.app.domain.usecase.matchmaking.DynamicRadiusExpansionUseCase
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sentinel kategori untuk "Lihat Semua" — menggabungkan kandidat dari semua kategori. */
const val ALL_CATEGORIES_ID = 0

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
    val maxDistanceFilter: Double = BantuinConfig.MAX_RADIUS_KM,
    val minRatingFilter: Double = 0.0,
    val onlyTrusted: Boolean = false,
    val sortOption: ProviderSortOption = ProviderSortOption.RECOMMENDED,
    val rawCandidates: List<ProviderCandidate> = emptyList(),
    val matchResult: MatchResult = MatchResult(emptyList(), emptyList()),
    val filteredList: List<RankedProvider> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // ── SCALE: Dynamic Radius Expansion ──────────────────────────
    val effectiveRadius: Double = BantuinConfig.MAX_RADIUS_KM_DEFAULT,
    val expansionState: RadiusExpansionState = RadiusExpansionState.NORMAL,
    val potentialCountIfExpanded: Int = 0,
    // ── SCALE: Bookmark / Mitra Favorit ──────────────────────────
    val onlyBookmarked: Boolean = false,
    val bookmarkedProviderIds: Set<String> = emptySet(),
    val bookmarkList: List<BookmarkedProvider> = emptyList()
) {
    val activeFilterCount: Int
        get() = listOf(
            (maxDistanceFilter < BantuinConfig.MAX_RADIUS_KM),
            minRatingFilter > 0.0,
            onlyTrusted,
            onlyBookmarked
        ).count { it }

    val hasActiveFilterOrSort: Boolean
        get() = activeFilterCount > 0 || sortOption != ProviderSortOption.RECOMMENDED
}

class RecommendationViewModel(
    private val catalogRepository: CatalogRepository,
    private val matchmakingUseCase: MatchmakingUseCase = MatchmakingUseCase(),
    private val expansionUseCase: DynamicRadiusExpansionUseCase = DynamicRadiusExpansionUseCase(),
    private val bookmarkUseCase: BookmarkUseCase = BookmarkUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentCustomerId: String = "user_demo"

    fun initialize(
        categoryId: Int,
        customerLocation: GeoPoint,
        customerId: String = "user_demo"
    ) {
        currentCustomerId = customerId
        _uiState.update {
            it.copy(
                categoryId = categoryId,
                customerLocation = customerLocation
            )
        }
        loadCandidates(categoryId, customerLocation)
        refreshBookmarks()
    }

    fun loadCandidates(
        categoryId: Int = _uiState.value.categoryId,
        customerLocation: GeoPoint = _uiState.value.customerLocation
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val catsRes = catalogRepository.categories()
                val categories = (catsRes as? AppResult.Success)?.data.orEmpty()
                val resolvedCategory = if (categoryId == ALL_CATEGORIES_ID) {
                    ServiceCategory(
                        id = ALL_CATEGORIES_ID,
                        name = "Semua Layanan",
                        iconKey = "grid_view",
                        defaultDurationMin = 60,
                        description = "Semua layanan teknis yang tersedia"
                    )
                } else {
                    categories.find { it.id == categoryId } ?: _uiState.value.category
                }

                val res: AppResult<List<ProviderCandidate>> = if (categoryId == ALL_CATEGORIES_ID) {
                    val results = categories.map { category ->
                        async { catalogRepository.candidates(category.id) }
                    }.awaitAll()
                    val firstError = results.filterIsInstance<AppResult.Error>().firstOrNull()
                    if (firstError != null) {
                        firstError
                    } else {
                        AppResult.Success(
                            results.filterIsInstance<AppResult.Success<List<ProviderCandidate>>>()
                                .flatMap { it.data }
                                .distinctBy { it.providerId }
                        )
                    }
                } else {
                    catalogRepository.candidates(categoryId)
                }

                when (res) {
                    is AppResult.Success -> {
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

    fun setOnlyBookmarked(onlyBookmarked: Boolean) {
        _uiState.update { it.copy(onlyBookmarked = onlyBookmarked) }
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
        _uiState.update {
            it.copy(
                expansionState = RadiusExpansionState.NORMAL,
                onlyBookmarked = false
            )
        }
        applyFiltersAndSort(
            maxDistance = BantuinConfig.MAX_RADIUS_KM,
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

    fun acceptRadiusExpansion() {
        val state = _uiState.value
        if (state.expansionState != RadiusExpansionState.SUGGESTING_EXPANSION) return
        _uiState.update {
            it.copy(
                maxDistanceFilter = BantuinConfig.MAX_RADIUS_KM_EXPANDED,
                expansionState = RadiusExpansionState.EXPANDED
            )
        }
        recomputeFiltered(allowExpanded = true)
    }

    fun dismissRadiusExpansion() {
        _uiState.update {
            it.copy(expansionState = RadiusExpansionState.NORMAL)
        }
    }

    fun toggleBookmark(candidate: ProviderCandidate) {
        bookmarkUseCase.toggle(currentCustomerId, candidate, _uiState.value.categoryId)
        refreshBookmarks()
        if (_uiState.value.onlyBookmarked) {
            recomputeFiltered()
        }
    }

    fun isBookmarked(providerId: String): Boolean =
        _uiState.value.bookmarkedProviderIds.contains(providerId)

    fun refreshBookmarks() {
        val bookmarks = bookmarkUseCase.getBookmarks(currentCustomerId)
        _uiState.update {
            it.copy(
                bookmarkedProviderIds = bookmarks.map { b -> b.providerId }.toSet(),
                bookmarkList = bookmarks
            )
        }
    }

    private fun recomputeRankings(
        category: ServiceCategory,
        candidates: List<ProviderCandidate>,
        location: GeoPoint,
        maxDistance: Double,
        minRating: Double,
        onlyTrusted: Boolean,
        sortOption: ProviderSortOption,
        allowExpanded: Boolean = _uiState.value.expansionState == RadiusExpansionState.EXPANDED
    ) {
        val expandedResult = expansionUseCase(
            customer = location,
            candidates = candidates,
            defaultRadiusKm = maxDistance,
            expandedRadiusKm = BantuinConfig.MAX_RADIUS_KM_EXPANDED,
            allowExpanded = allowExpanded
        )
        val matchResult = expandedResult.matchResult
        val filteredAndSorted = sortRanked(
            filterRanked(
                ranked = matchResult.ranked,
                maxDistance = expandedResult.effectiveRadiusKm,
                minRating = minRating,
                onlyTrusted = onlyTrusted,
                onlyBookmarked = _uiState.value.onlyBookmarked,
                bookmarkedIds = _uiState.value.bookmarkedProviderIds
            ),
            sortOption
        )
        _uiState.update {
            it.copy(
                category = category,
                rawCandidates = candidates,
                matchResult = matchResult,
                filteredList = filteredAndSorted,
                effectiveRadius = expandedResult.effectiveRadiusKm,
                expansionState = expandedResult.expansionState,
                potentialCountIfExpanded = expandedResult.potentialCountIfExpanded,
                isLoading = false
            )
        }
    }

    private fun recomputeFiltered(
        allowExpanded: Boolean = _uiState.value.expansionState == RadiusExpansionState.EXPANDED
    ) {
        val state = _uiState.value
        recomputeRankings(
            category = state.category,
            candidates = state.rawCandidates,
            location = state.customerLocation,
            maxDistance = state.maxDistanceFilter,
            minRating = state.minRatingFilter,
            onlyTrusted = state.onlyTrusted,
            sortOption = state.sortOption,
            allowExpanded = allowExpanded
        )
    }

    private fun filterRanked(
        ranked: List<RankedProvider>,
        maxDistance: Double,
        minRating: Double,
        onlyTrusted: Boolean,
        onlyBookmarked: Boolean = false,
        bookmarkedIds: Set<String> = emptySet()
    ): List<RankedProvider> {
        return ranked.filter { item ->
            val distPass = item.distanceKm <= maxDistance
            val ratingPass = (item.candidate.ratingAvg ?: 0.0) >= minRating
            val trustedPass = !onlyTrusted || item.candidate.badge == ProviderBadge.TRUSTED
            val bookmarkPass = !onlyBookmarked || bookmarkedIds.contains(item.candidate.providerId)
            distPass && ratingPass && trustedPass && bookmarkPass
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
