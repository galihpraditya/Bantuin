package id.bantuin.app.ui.provider.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderHomeUiState(
    val user: UserProfile? = null,
    val provider: ProviderCandidate? = null,
    val trustResult: TrustResult? = null,
    val pendingCount: Int = 0,
    val todayOrdersCount: Int = 0,
    val isOnline: Boolean = true,
    val isLoading: Boolean = false
)

class ProviderHomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val metricsRepository: MetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderHomeUiState())
    val uiState: StateFlow<ProviderHomeUiState> = _uiState.asStateFlow()

    private val trustScore = TrustScoreUseCase()
    private var trustJob: Job? = null

    init {
        loadData()
    }

    fun loadData(providerId: String = "p01") {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { state ->
                    val trust = if (state.user?.id == user?.id) state.trustResult else null
                    state.copy(user = user, trustResult = trust)
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val candidatesRes = catalogRepository.candidates(2) // AC category
            val ordersRes = bookingRepository.myBookingsProvider()

            val provider = if (candidatesRes is AppResult.Success) {
                candidatesRes.data.find { it.providerId == providerId } ?: candidatesRes.data.firstOrNull()
            } else null

            val orders = if (ordersRes is AppResult.Success) ordersRes.data else emptyList()
            val pending = orders.count { it.status == BookingStatus.PENDING }

            _uiState.update {
                it.copy(
                    provider = provider,
                    pendingCount = pending,
                    todayOrdersCount = orders.size,
                    isLoading = false
                )
            }
        }
    }

    fun refreshTrustScore(providerId: String) {
        trustJob?.cancel()
        trustJob = viewModelScope.launch {
            val input = withContext(Dispatchers.IO) {
                metricsRepository.trustInput(providerId, BantuinConfig.TRUST_WINDOW_DAYS)
            }
            if (input is AppResult.Success) {
                val trust = withContext(Dispatchers.Default) { trustScore(input.data) }
                _uiState.update { if (it.user?.id == providerId) it.copy(trustResult = trust) else it }
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

    fun switchToCustomer(onSuccess: () -> Unit) =
        switchToCustomer(onAllowed = onSuccess, onUnauthorized = {})
}
