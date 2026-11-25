package id.bantuin.app.ui.provider.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

data class ProviderDashboardUiState(
    val user: UserProfile? = null,
    val providerId: String = "",
    val providerName: String = "Mitra Bantuin",
    val providerSpecialty: String = "Mitra Bantuin",
    val providerAddress: String = "Malang & Sekitarnya",
    val trustInput: TrustInput? = null,
    val trustResult: TrustResult? = null,
    val snapshots: List<MetricSnapshot> = emptyList(),
    val latestPendingOrder: BookingItem? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Nilai simulasi what-if anomali
    val simRating: Double = 4.9,
    val simLateCount: Int = 0,
    val simCancelCount: Int = 0,
    val simResult: TrustResult? = null,
    val isSimulationActive: Boolean = false,
    val isRespondingOrder: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProviderDashboardViewModel(
    private val metricsRepository: MetricsRepository,
    private val authRepository: AuthRepository,
    private val catalogRepository: CatalogRepository,
    private val bookingRepository: BookingRepository? = catalogRepository as? BookingRepository,
    private val themeStore: ThemePreferenceStore = InMemoryThemeStore(),
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProviderDashboardUiState(themeMode = themeStore.themeModeValue)
    )
    val uiState: StateFlow<ProviderDashboardUiState> = _uiState.asStateFlow()

    private var ordersJob: Job? = null

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            themeStore.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
                loadData(user?.id)
            }
        }
        bookingRepository?.let { repo ->
            ordersJob?.cancel()
            ordersJob = viewModelScope.launch {
                repo.observeProviderBookings().collect { list ->
                    val latestPending = list.firstOrNull { it.status == BookingStatus.PENDING }
                    _uiState.update { it.copy(latestPendingOrder = latestPending) }
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeStore.setThemeMode(mode)
        }
    }

    fun loadData(providerId: String? = null) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() } ?: user?.id
            if (resolvedId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        providerId = "",
                        providerName = it.user?.fullName ?: "Mitra Bantuin",
                        providerSpecialty = "Mitra Bantuin",
                        isLoading = false
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    user = it.user ?: user,
                    providerId = resolvedId,
                    providerName = it.user?.fullName ?: user?.fullName ?: "Mitra Bantuin",
                    isLoading = true,
                    errorMessage = null
                )
            }

            coroutineScope {
                val profileDeferred = async { catalogRepository.providerProfile(resolvedId) }
                val servicesDeferred = async { catalogRepository.servicesOf(resolvedId) }
                val inputDeferred = async { metricsRepository.trustInput(resolvedId, 30) }
                val historyDeferred = async { metricsRepository.history(resolvedId) }
                val ordersDeferred = bookingRepository?.let { repo ->
                    async {
                        repo.expirePending()
                        repo.myBookingsProvider()
                    }
                }

                val profileRes = profileDeferred.await()
                val servicesRes = servicesDeferred.await()
                val inputRes = inputDeferred.await()
                val historyRes = historyDeferred.await()
                val ordersRes = ordersDeferred?.await()

                val profile = (profileRes as? AppResult.Success)?.data
                val specialty = (servicesRes as? AppResult.Success)?.data?.firstOrNull()?.name
                    ?: profile?.bio?.takeIf { it.isNotBlank() }
                    ?: "Mitra Bantuin"

                val input = (inputRes as? AppResult.Success)?.data
                val snapshots = (historyRes as? AppResult.Success)?.data ?: emptyList()
                val result = input?.let { trustScoreUseCase.calculate(it) }
                val latestPending = (ordersRes as? AppResult.Success)?.data
                    ?.firstOrNull { it.status == BookingStatus.PENDING }

                val tolerance = BantuinConfig.LATE_TOLERANCE_MIN.minutes
                val avgRating = input?.ratings?.takeIf { it.isNotEmpty() }?.average() ?: 4.9
                val lateCount = input?.completed?.count { it.startedAt > it.startAt + tolerance } ?: 0
                val cancelCount = input?.mitraCancelledCount ?: 0

                val err = (inputRes as? AppResult.Error)?.message

                _uiState.update {
                    it.copy(
                        providerSpecialty = specialty,
                        providerAddress = profile?.address?.takeIf { a -> a.isNotBlank() } ?: "Malang & Sekitarnya",
                        trustInput = input,
                        trustResult = result,
                        snapshots = snapshots,
                        latestPendingOrder = latestPending,
                        simRating = avgRating,
                        simLateCount = lateCount,
                        simCancelCount = cancelCount,
                        simResult = if (it.isSimulationActive) it.simResult else result,
                        isLoading = false,
                        errorMessage = err
                    )
                }
            }
        }
    }

    fun respondToPendingOrder(bookingId: String, accept: Boolean) {
        val repo = bookingRepository ?: return
        val newStatus = if (accept) BookingStatus.DIKONFIRMASI else BookingStatus.DITOLAK
        viewModelScope.launch {
            _uiState.update { it.copy(isRespondingOrder = true, errorMessage = null) }
            when (val res = repo.updateStatus(bookingId, newStatus)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isRespondingOrder = false) }
                    loadData(_uiState.value.providerId)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isRespondingOrder = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun switchToCustomer(
        onAllowed: () -> Unit,
        onUnauthorized: () -> Unit
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            if (user?.roles?.contains("customer") != true) {
                authRepository.signOut()
                onUnauthorized()
                return@launch
            }
            authRepository.setActiveRole("customer")
            onAllowed()
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }

    fun updateSimulation(rating: Double, lateCount: Int, cancelCount: Int) {
        val baseInput = _uiState.value.trustInput ?: return
        val tolerance = BantuinConfig.LATE_TOLERANCE_MIN.minutes

        val simulatedCompleted = baseInput.completed.mapIndexed { idx, job ->
            if (idx < lateCount) {
                job.copy(startedAt = job.startAt + tolerance + 5.minutes)
            } else {
                job.copy(startedAt = job.startAt)
            }
        }

        val roundedRating = rating.roundToInt().coerceIn(1, 5)
        val simulatedRatings = List(baseInput.ratings.size.coerceAtLeast(1)) { roundedRating }

        val simulatedInput = baseInput.copy(
            completed = simulatedCompleted,
            mitraCancelledCount = cancelCount,
            ratings = simulatedRatings
        )

        val newResult = trustScoreUseCase.calculate(simulatedInput)

        _uiState.update {
            it.copy(
                simRating = rating,
                simLateCount = lateCount,
                simCancelCount = cancelCount,
                simResult = newResult,
                isSimulationActive = true
            )
        }
    }

    fun resetSimulation() {
        val baseInput = _uiState.value.trustInput
        val baseResult = _uiState.value.trustResult
        val tolerance = BantuinConfig.LATE_TOLERANCE_MIN.minutes
        val avgRating = baseInput?.ratings?.takeIf { it.isNotEmpty() }?.average() ?: 4.9
        val lateCount = baseInput?.completed?.count { it.startedAt > it.startAt + tolerance } ?: 0
        val cancelCount = baseInput?.mitraCancelledCount ?: 0

        _uiState.update {
            it.copy(
                simRating = avgRating,
                simLateCount = lateCount,
                simCancelCount = cancelCount,
                simResult = baseResult,
                isSimulationActive = false
            )
        }
    }
}
