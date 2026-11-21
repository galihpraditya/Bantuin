package id.bantuin.app.ui.provider.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderDashboardUiState(
    val providerId: String = "p01",
    val trustInput: TrustInput? = null,
    val trustResult: TrustResult? = null,
    val snapshots: List<MetricSnapshot> = emptyList(),
    // Nilai simulasi
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
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderDashboardUiState())
    val uiState: StateFlow<ProviderDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(providerId: String = "p01") {
        viewModelScope.launch {
            _uiState.update { it.copy(providerId = providerId, isLoading = true, errorMessage = null) }
            val inputRes = metricsRepository.trustInput(providerId, 30)
            val historyRes = metricsRepository.history(providerId)

            val input = if (inputRes is AppResult.Success) inputRes.data else null
            val snapshots = if (historyRes is AppResult.Success) historyRes.data else emptyList()

            val result = if (input != null) {
                trustScoreUseCase(input)
            } else null

            _uiState.update {
                it.copy(
                    trustInput = input,
                    trustResult = result,
                    snapshots = snapshots,
                    simRating = input?.completedJobs?.map { j -> j.rating }?.average() ?: 4.9,
                    simLateCount = input?.completedJobs?.count { j -> j.isLate } ?: 0,
                    simCancelCount = input?.providerCancelCount ?: 0,
                    simResult = result,
                    isLoading = false
                )
            }
        }
    }

    fun updateSimulation(rating: Double, lateCount: Int, cancelCount: Int) {
        val baseInput = _uiState.value.trustInput ?: return
        val totalJobs = baseInput.completedJobs.size.coerceAtLeast(1)

        val simulatedInput = baseInput.copy(
            completedJobs = baseInput.completedJobs.mapIndexed { idx, job ->
                job.copy(
                    rating = rating,
                    isLate = idx < lateCount
                )
            },
            providerCancelCount = cancelCount
        )

        val newResult = trustScoreUseCase(simulatedInput)

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
        _uiState.update {
            it.copy(
                simRating = baseInput?.completedJobs?.map { j -> j.rating }?.average() ?: 4.9,
                simLateCount = baseInput?.completedJobs?.count { j -> j.isLate } ?: 0,
                simCancelCount = baseInput?.providerCancelCount ?: 0,
                simResult = baseResult,
                isSimulationActive = false
            )
        }
    }
}
