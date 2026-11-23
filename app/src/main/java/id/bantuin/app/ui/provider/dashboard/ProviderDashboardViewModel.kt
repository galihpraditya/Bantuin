package id.bantuin.app.ui.provider.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.dispatcher.AppDispatchers
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.CompletionStatus
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.TransactionOutcome
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.trust.RecalculateProviderMetricsUseCase
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

data class ProviderDashboardUiState(
    val user: UserProfile? = null,
    val providerId: String = "",
    val providerName: String = "Mitra Bantuin",
    val providerSpecialty: String = "Mitra Bantuin",
    val trustInput: TrustInput? = null,
    val trustResult: TrustResult? = null,
    val snapshots: List<MetricSnapshot> = emptyList(),
    val transactions: List<TransactionOutcome> = emptyList(),
    val historyFilter: CompletionStatus? = null,
    val demoExamples: List<TrustExample> = emptyList(),
    val checkedAt: Instant? = null,
    val simRating: Double = 4.9,
    val simLateCount: Int = 0,
    val simCancelCount: Int = 0,
    val simResult: TrustResult? = null,
    val isSimulationActive: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val historyError: String? = null
)

data class TrustExample(val name: String, val result: TrustResult)

class ProviderDashboardViewModel(
    private val metricsRepository: MetricsRepository,
    private val authRepository: AuthRepository? = null,
    private val catalogRepository: CatalogRepository? = null,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase(),
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderDashboardUiState())
    val uiState = _uiState.asStateFlow()
    private val recalculate = RecalculateProviderMetricsUseCase(metricsRepository)
    private var request: Job? = null

    init {
        if (authRepository != null) {
            viewModelScope.launch {
                authRepository.currentUser().collect { user ->
                    _uiState.update { it.copy(user = user) }
                    loadData(user?.id)
                }
            }
        }
    }

    fun loadData(providerId: String? = null, background: Boolean = false) {
        val resolvedId = providerId?.takeIf { it.isNotBlank() }
        if (resolvedId != null && _uiState.value.providerId == resolvedId && request?.isActive == true) return
        request?.cancel()
        request = viewModelScope.launch {
            val user = authRepository?.currentUser()?.firstOrNull()
            val id = resolvedId ?: user?.id
            if (id.isNullOrBlank()) {
                _uiState.update { it.copy(providerId = "", providerName = it.user?.fullName ?: "Mitra Bantuin", isLoading = false) }
                return@launch
            }
            _uiState.update {
                it.copy(providerId = id, providerName = it.user?.fullName ?: user?.fullName ?: "Mitra Bantuin",
                    isLoading = !background, errorMessage = null)
            }

            val servicesRes = catalogRepository?.servicesOf(id)
            val specialty = (servicesRes as? AppResult.Success)?.data?.firstOrNull()?.name ?: "Mitra Bantuin"
            val inputRes = withContext(dispatchers.io) { metricsRepository.trustInput(id, BantuinConfig.TRUST_WINDOW_DAYS) }
            val historyRes = withContext(dispatchers.io) { metricsRepository.history(id) }
            val transactionsRes = withContext(dispatchers.io) { metricsRepository.transactionHistory(id, BantuinConfig.TRUST_WINDOW_DAYS) }
            val input = (inputRes as? AppResult.Success)?.data
            val result = input?.let { withContext(dispatchers.default) { trustScoreUseCase(it) } }
            val avgRating = if ((input?.ratingCount ?: 0) > 0)
                input!!.ratingTotal.toDouble() / input.ratingCount else 4.9
            val lateCount = input?.lateCount ?: 0
            val cancelCount = input?.cancelledCount ?: 0
            val snapshots = (historyRes as? AppResult.Success)?.data ?: emptyList()
            val transactions = (transactionsRes as? AppResult.Success)?.data ?: emptyList()
            _uiState.update {
                it.copy(providerId = id, providerName = it.user?.fullName ?: user?.fullName ?: "Mitra Bantuin",
                    providerSpecialty = specialty, trustInput = input, trustResult = result,
                    snapshots = snapshots, transactions = transactions,
                    simRating = avgRating, simLateCount = lateCount, simCancelCount = cancelCount,
                    simResult = result, checkedAt = if (result == null) it.checkedAt else Clock.System.now(),
                    isLoading = false, errorMessage = (inputRes as? AppResult.Error)?.message,
                    historyError = (transactionsRes as? AppResult.Error)?.message)
            }
        }
    }

    fun refresh() {
        val id = _uiState.value.providerId
        if (id.isBlank() || _uiState.value.isRefreshing) return
        request?.cancel()
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        request = viewModelScope.launch {
            when (val result = withContext(dispatchers.io) { recalculate(id) }) {
                is AppResult.Success -> loadData(id, background = false)
                is AppResult.Error -> _uiState.update { it.copy(errorMessage = result.message, isRefreshing = false) }
                AppResult.Loading -> _uiState.update { it.copy(isRefreshing = false) }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun filterHistory(status: CompletionStatus?) {
        _uiState.update { it.copy(historyFilter = status) }
    }

    fun switchToCustomer(onAllowed: () -> Unit, onUnauthorized: () -> Unit) {
        viewModelScope.launch {
            val user = authRepository?.currentUser()?.firstOrNull()
            if (user?.roles?.contains("customer") != true) {
                authRepository?.signOut()
                onUnauthorized()
                return@launch
            }
            authRepository.setActiveRole("customer")
            onAllowed()
        }
    }

    fun switchToCustomer(onSuccess: () -> Unit) = switchToCustomer(onAllowed = onSuccess, onUnauthorized = {})
    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch { authRepository?.signOut(); onComplete() }
    }

    fun updateSimulation(rating: Double, lateCount: Int, cancelCount: Int) {
        val baseInput = _uiState.value.trustInput ?: return
        val rounded = rating.roundToInt().coerceIn(1, 5)
        val simulated = baseInput.copy(
            lateCount = lateCount,
            cancelledCount = cancelCount,
            ratingCount = baseInput.ratingCount.coerceAtLeast(1),
            ratingTotal = rounded.toLong() * baseInput.ratingCount.coerceAtLeast(1)
        )
        val result = trustScoreUseCase(simulated)
        _uiState.update {
            it.copy(simRating = rating, simLateCount = lateCount, simCancelCount = cancelCount,
                simResult = result, isSimulationActive = true)
        }
    }

    fun resetSimulation() {
        val base = _uiState.value.trustInput
        val avg = if ((base?.ratingCount ?: 0) > 0)
            base!!.ratingTotal.toDouble() / base.ratingCount else 4.9
        val late = base?.lateCount ?: 0
        val cancel = base?.cancelledCount ?: 0
        _uiState.update {
            it.copy(simRating = avg, simLateCount = late, simCancelCount = cancel,
                simResult = it.trustResult, isSimulationActive = false)
        }
    }
}
