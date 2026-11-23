package id.bantuin.app.ui.provider.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
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
    private val authRepository: AuthRepository,
    private val catalogRepository: CatalogRepository,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderDashboardUiState())
    val uiState: StateFlow<ProviderDashboardUiState> = _uiState.asStateFlow()

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

    /**
     * Identitas mitra selalu diambil dari akun yang sedang login. Tidak ada lagi
     * fallback ke "p01"/Budi Santoso sehingga akun mitra lain tidak menampilkan
     * profil dan metrik orang lain.
     */
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

            val servicesRes = catalogRepository.servicesOf(resolvedId)
            val specialty = when (servicesRes) {
                is AppResult.Success -> servicesRes.data.firstOrNull()?.name ?: "Mitra Bantuin"
                else -> "Mitra Bantuin"
            }
            val inputRes = metricsRepository.trustInput(resolvedId, 30)
            val historyRes = metricsRepository.history(resolvedId)

            val input = if (inputRes is AppResult.Success) inputRes.data else null
            val snapshots = if (historyRes is AppResult.Success) historyRes.data else emptyList()
            val result = input?.let { trustScoreUseCase(it) }

            val tolerance = BantuinConfig.LATE_TOLERANCE_MIN.minutes
            val avgRating = input?.ratings?.takeIf { it.isNotEmpty() }?.average() ?: 4.9
            val lateCount = input?.completed?.count { it.startedAt > it.startAt + tolerance } ?: 0
            val cancelCount = input?.mitraCancelledCount ?: 0

            _uiState.update {
                it.copy(
                    providerSpecialty = specialty,
                    trustInput = input,
                    trustResult = result,
                    snapshots = snapshots,
                    simRating = avgRating,
                    simLateCount = lateCount,
                    simCancelCount = cancelCount,
                    simResult = result,
                    isLoading = false
                )
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
