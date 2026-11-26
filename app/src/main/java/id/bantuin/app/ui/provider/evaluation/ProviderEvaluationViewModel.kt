package id.bantuin.app.ui.provider.evaluation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.usecase.trust.RecalculateProviderMetricsUseCase
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.Locale

data class ProviderEvaluationUiState(
    val providerId: String = "",
    val trustInput: TrustInput? = null,
    val trustResult: TrustResult? = null,
    val tierTitle: String = "Reguler",
    val multiplierText: String = "1.0x Prioritas Normal",
    val visibilityText: String = "",
    val lastSyncLabel: String = "",
    val isLoading: Boolean = false,
    val isRecalculating: Boolean = false,
    val recalculatedRecently: Boolean = false,
    val errorMessage: String? = null
)

class ProviderEvaluationViewModel(
    private val metricsRepository: MetricsRepository,
    private val authRepository: AuthRepository,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderEvaluationUiState())
    val uiState: StateFlow<ProviderEvaluationUiState> = _uiState.asStateFlow()

    private val recalculateUseCase = RecalculateProviderMetricsUseCase(metricsRepository, trustScoreUseCase)

    init {
        loadEvaluation()
    }

    fun loadEvaluation(providerId: String? = null) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() } ?: user?.id
            if (resolvedId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Akun mitra tidak ditemukan.") }
                return@launch
            }
            _uiState.update { it.copy(providerId = resolvedId, isLoading = true, errorMessage = null) }

            when (val res = metricsRepository.trustInput(resolvedId, BantuinConfig.TRUST_WINDOW_DAYS)) {
                is AppResult.Success -> {
                    val evaluated = trustScoreUseCase.calculate(res.data)
                    applyEvaluatedState(
                        input = res.data,
                        result = evaluated,
                        syncLabel = "Tersinkron pukul ${TimeUtils.formatInstantToTime(Clock.System.now())}",
                        recalculated = false
                    )
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun recalculate() {
        val providerId = _uiState.value.providerId
        if (providerId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRecalculating = true, errorMessage = null) }
            when (val res = recalculateUseCase(providerId)) {
                is AppResult.Success -> {
                    applyEvaluatedState(
                        input = _uiState.value.trustInput,
                        result = res.data,
                        syncLabel = "Dihitung ulang pukul ${TimeUtils.formatInstantToTime(Clock.System.now())}",
                        recalculated = true
                    )
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isRecalculating = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    private fun applyEvaluatedState(
        input: TrustInput?,
        result: TrustResult,
        syncLabel: String,
        recalculated: Boolean
    ) {
        val tier = when {
            result.flagAnomaly -> "Evaluasi Anomali"
            else -> "Mitra Terpercaya"
        }
        val multiplier = if (result.flagAnomaly) {
            String.format(Locale.US, "%.1fx Penalti Visibilitas", BantuinConfig.ANOMALY_PENALTY_FACTOR)
        } else {
            "1.0x Prioritas Penuh"
        }
        val visibility = if (result.flagAnomaly) {
            "Skor matchmaking saat ini dikalikan ${BantuinConfig.ANOMALY_PENALTY_FACTOR}x hingga rasio ketepatan & pembatalan membaik."
        } else {
            "Akun bebas pelanggaran. Skor matchmaking Anda tampil penuh tanpa penalti di radius ${BantuinConfig.MAX_RADIUS_KM.toInt()} km."
        }

        _uiState.update {
            it.copy(
                trustInput = input,
                trustResult = result,
                tierTitle = tier,
                multiplierText = multiplier,
                visibilityText = visibility,
                lastSyncLabel = syncLabel,
                isLoading = false,
                isRecalculating = false,
                recalculatedRecently = recalculated,
                errorMessage = null
            )
        }
    }
}
