package id.bantuin.app.ui.provider.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.ProviderRevenueAnalytics
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.repository.WalletRepository
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
    val revenueAnalytics: ProviderRevenueAnalytics = ProviderRevenueAnalytics(),
    val trustInput: TrustInput? = null,
    val trustResult: TrustResult? = null,
    val snapshots: List<MetricSnapshot> = emptyList(),
    // Nilai simulasi what-if anomali
    val simRating: Double = 4.9,
    val simLateCount: Int = 0,
    val simCancelCount: Int = 0,
    val simResult: TrustResult? = null,
    val isSimulationActive: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProviderDashboardViewModel(
    private val metricsRepository: MetricsRepository,
    private val authRepository: AuthRepository,
    private val catalogRepository: CatalogRepository,
    private val walletRepository: WalletRepository? = catalogRepository as? WalletRepository,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderDashboardUiState())
    val uiState: StateFlow<ProviderDashboardUiState> = _uiState.asStateFlow()

    private var walletJob: Job? = null

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
                loadData(user?.id)
            }
        }
    }

    private fun observeWallet(providerId: String) {
        walletRepository?.let { repo ->
            walletJob?.cancel()
            walletJob = viewModelScope.launch {
                repo.observeProviderRevenue(providerId).collect { analytics ->
                    _uiState.update { it.copy(revenueAnalytics = analytics) }
                }
            }
        }
    }

    fun loadData(providerId: String? = null) {
        viewModelScope.launch {
            val user = _uiState.value.user ?: authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() }
                ?: user?.id?.takeIf { it.isNotBlank() }
            if (resolvedId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Sesi mitra belum tersedia."
                    )
                }
                return@launch
            }

            observeWallet(resolvedId)

            _uiState.update {
                it.copy(
                    providerId = resolvedId,
                    providerName = user?.fullName?.takeIf { n -> n.isNotBlank() } ?: "Mitra Bantuin",
                    isLoading = true,
                    errorMessage = null
                )
            }

            coroutineScope {
                val inputDeferred = async { metricsRepository.trustInput(resolvedId, BantuinConfig.TRUST_WINDOW_DAYS) }
                val historyDeferred = async { metricsRepository.history(resolvedId) }
                val profileDeferred = async { catalogRepository.providerProfile(resolvedId) }
                val servicesDeferred = async { catalogRepository.servicesOf(resolvedId) }

                val inputRes = inputDeferred.await()
                val historyRes = historyDeferred.await()
                val profileRes = profileDeferred.await()
                val servicesRes = servicesDeferred.await()

                val profile = (profileRes as? AppResult.Success)?.data
                val firstService = (servicesRes as? AppResult.Success)?.data?.firstOrNull()?.name
                val specialty = firstService?.takeIf { it.isNotBlank() }
                    ?: profile?.bio?.takeIf { it.isNotBlank() }
                    ?: "Mitra Bantuin"

                val input = (inputRes as? AppResult.Success)?.data
                val snapshots = (historyRes as? AppResult.Success)?.data ?: emptyList()
                val result = input?.let { trustScoreUseCase.calculate(it) }

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

    fun updateSimulation(rating: Double, lateCount: Int, cancelCount: Int) {
        val baseInput = _uiState.value.trustInput ?: return
        val tolerance = BantuinConfig.LATE_TOLERANCE_MIN.minutes

        val simulatedCompleted = baseInput.completed.mapIndexed { idx, job ->
            if (idx < lateCount) {
                job.copy(startedAt = job.startAt + tolerance + 5.minutes, outcome = null)
            } else {
                job.copy(startedAt = job.startAt, outcome = null)
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
