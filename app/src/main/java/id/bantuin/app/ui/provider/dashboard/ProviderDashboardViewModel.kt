package id.bantuin.app.ui.provider.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.dispatcher.AppDispatchers
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.CompletionStatus
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.TransactionOutcome
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.usecase.trust.RecalculateProviderMetricsUseCase
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class ProviderDashboardUiState(
    val providerId: String = "",
    val trustResult: TrustResult? = null,
    val snapshots: List<MetricSnapshot> = emptyList(),
    val transactions: List<TransactionOutcome> = emptyList(),
    val historyFilter: CompletionStatus? = null,
    val demoExamples: List<TrustExample> = emptyList(),
    val checkedAt: Instant? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val historyError: String? = null
)

data class TrustExample(val name: String, val result: TrustResult)

class ProviderDashboardViewModel(
    private val metricsRepository: MetricsRepository,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase(),
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderDashboardUiState())
    val uiState = _uiState.asStateFlow()
    private val recalculate = RecalculateProviderMetricsUseCase(metricsRepository)
    private var request: Job? = null

    fun loadData(providerId: String, background: Boolean = false) {
        if (_uiState.value.providerId != providerId) {
            request?.cancel()
            _uiState.value = ProviderDashboardUiState(providerId = providerId)
        } else if (request?.isActive == true) return

        request = viewModelScope.launch {
            load(providerId, background)
        }
    }

    fun refresh() {
        val providerId = _uiState.value.providerId.takeIf { it.isNotBlank() } ?: return
        if (_uiState.value.isRefreshing) return
        request?.cancel()
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        request = viewModelScope.launch {
            when (val result = withContext(dispatchers.io) { recalculate(providerId) }) {
                is AppResult.Success -> load(providerId, background = false)
                is AppResult.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                AppResult.Loading -> Unit
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun filterHistory(status: CompletionStatus?) {
        _uiState.update { it.copy(historyFilter = status) }
    }

    private suspend fun load(providerId: String, background: Boolean) = coroutineScope {
        if (!background && _uiState.value.trustResult == null) _uiState.update { it.copy(isLoading = true) }
        val days = BantuinConfig.TRUST_WINDOW_DAYS
        val inputs = async(dispatchers.io) { metricsRepository.trustInput(providerId, days) }
        val snapshots = async(dispatchers.io) { metricsRepository.history(providerId) }
        val transactions = async(dispatchers.io) { metricsRepository.transactionHistory(providerId, days) }

        val inputResult = inputs.await()
        val input = (inputResult as? AppResult.Success)?.data
        val result = input?.let { withContext(dispatchers.default) { trustScoreUseCase(it) } }
        val examples = if (input?.hasEvidence != true) {
            _uiState.value.demoExamples.ifEmpty { demoExamples(days) }
        } else emptyList()
        val snapshotsResult = snapshots.await()
        val transactionsResult = transactions.await()
        _uiState.update { state ->
            state.copy(
                trustResult = result ?: state.trustResult,
                snapshots = (snapshotsResult as? AppResult.Success)?.data ?: state.snapshots,
                transactions = (transactionsResult as? AppResult.Success)?.data ?: state.transactions,
                demoExamples = examples,
                checkedAt = if (result == null) state.checkedAt else Clock.System.now(),
                isLoading = false,
                errorMessage = (inputResult as? AppResult.Error)?.message,
                historyError = (transactionsResult as? AppResult.Error)?.message
            )
        }
    }

    private suspend fun demoExamples(days: Int): List<TrustExample> {
        val fixtures = listOf("p01" to "Budi Santoso", "p05" to "Joko Hendro")
        val inputs = withContext(dispatchers.io) {
            fixtures.mapNotNull { (id, name) ->
                val input = (metricsRepository.trustInput(id, days) as? AppResult.Success)?.data
                input?.takeIf { it.hasEvidence }?.let { name to it }
            }
        }
        return withContext(dispatchers.default) {
            inputs.map { (name, input) -> TrustExample(name, trustScoreUseCase(input)) }
        }
    }
}
