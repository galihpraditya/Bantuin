package id.bantuin.app.ui.customer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerHomeUiState(
    val user: UserProfile? = null,
    val customerAddress: String = "Jl. Ijen No. 25, Klojen, Kota Malang",
    val customerLocation: GeoPoint = BantuinConfig.MALANG_CENTER,
    val locationSourceLabel: String = "Lokasi default (ketuk untuk atur)",
    val isLocationFromGps: Boolean = false,
    val isDetectingGps: Boolean = false,
    val gpsErrorMessage: String? = null,
    val searchQuery: String = "",
    val categories: List<ServiceCategory> = emptyList(),
    val filteredCategories: List<ServiceCategory> = emptyList(),
    val nearbyProviders: List<RankedProvider> = emptyList(),
    val providerCategoryMap: Map<String, Int> = emptyMap(),
    val readyProviderCount: Int = 0,
    val showLocationDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CustomerHomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
    private val matchmakingUseCase: MatchmakingUseCase = MatchmakingUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerHomeUiState())
    val uiState: StateFlow<CustomerHomeUiState> = _uiState.asStateFlow()

    private var cachedProviders: List<ProviderCandidate> = emptyList()
    private var cachedCategoryMap: Map<String, Int> = emptyMap()

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
                    val cats = result.data
                    loadNearbyProviders(cats)
                    val query = _uiState.value.searchQuery
                    val filteredCats = filterCategories(cats, query)
                    val matchResult = matchmakingUseCase(_uiState.value.customerLocation, cachedProviders)
                    val filteredProviders = filterNearbyProviders(matchResult.ranked, query)

                    _uiState.update {
                        it.copy(
                            categories = cats,
                            filteredCategories = filteredCats,
                            nearbyProviders = filteredProviders,
                            providerCategoryMap = cachedCategoryMap,
                            readyProviderCount = matchResult.ranked.size,
                            isLoading = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    private suspend fun loadNearbyProviders(categories: List<ServiceCategory>) = coroutineScope {
        val byProviderId = linkedMapOf<String, ProviderCandidate>()
        val categoryMap = mutableMapOf<String, Int>()
        val results = categories.map { cat ->
            async { cat.id to catalogRepository.candidates(cat.id) }
        }.awaitAll()

        results.forEach { (catId, res) ->
            if (res is AppResult.Success) {
                res.data.forEach { candidate ->
                    byProviderId.putIfAbsent(candidate.providerId, candidate)
                    categoryMap.putIfAbsent(candidate.providerId, catId)
                }
            }
        }
        cachedProviders = byProviderId.values.toList()
        cachedCategoryMap = categoryMap
    }

    fun onSearchQueryChange(query: String) {
        val state = _uiState.value
        val filteredCats = filterCategories(state.categories, query)
        val matchResult = matchmakingUseCase(state.customerLocation, cachedProviders)
        val filteredProviders = filterNearbyProviders(matchResult.ranked, query)
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredCategories = filteredCats,
                nearbyProviders = filteredProviders
            )
        }
    }

    fun updateLocation(address: String, location: GeoPoint, fromGps: Boolean = false) {
        val state = _uiState.value
        val matchResult = matchmakingUseCase(location, cachedProviders)
        val filteredProviders = filterNearbyProviders(matchResult.ranked, state.searchQuery)
        _uiState.update {
            it.copy(
                customerAddress = address,
                customerLocation = location,
                locationSourceLabel = if (fromGps) "Terdeteksi GPS akurat" else "Titik peta dipilih",
                isLocationFromGps = fromGps,
                isDetectingGps = false,
                gpsErrorMessage = null,
                nearbyProviders = filteredProviders,
                readyProviderCount = matchResult.ranked.size,
                showLocationDialog = false
            )
        }
    }

    fun setDetectingGps(detecting: Boolean) {
        _uiState.update {
            it.copy(
                isDetectingGps = detecting,
                gpsErrorMessage = if (detecting) null else it.gpsErrorMessage
            )
        }
    }

    fun setGpsError(message: String?) {
        _uiState.update { it.copy(isDetectingGps = false, gpsErrorMessage = message) }
    }

    fun setShowLocationDialog(show: Boolean) {
        _uiState.update { it.copy(showLocationDialog = show, gpsErrorMessage = null) }
    }

    /**
     * Tombol "mode mitra" di beranda memakai aturan yang sama dengan halaman profil:
     * akun tanpa role provider tidak boleh masuk ke portal mitra dan diarahkan login.
     */
    fun switchToProvider(
        onAllowed: () -> Unit,
        onUnauthorized: () -> Unit
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            if (user?.roles?.contains("provider") != true) {
                authRepository.signOut()
                onUnauthorized()
                return@launch
            }
            authRepository.setActiveRole("provider")
            onAllowed()
        }
    }

    private fun filterCategories(categories: List<ServiceCategory>, query: String): List<ServiceCategory> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return categories
        return categories.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.description.contains(trimmed, ignoreCase = true)
        }
    }

    private fun filterNearbyProviders(ranked: List<RankedProvider>, query: String): List<RankedProvider> {
        val trimmed = query.trim()
        val matched = if (trimmed.isBlank()) {
            ranked
        } else {
            ranked.filter {
                it.candidate.name.contains(trimmed, ignoreCase = true) ||
                    it.candidate.bio.contains(trimmed, ignoreCase = true)
            }
        }
        return matched.take(6)
    }
}
