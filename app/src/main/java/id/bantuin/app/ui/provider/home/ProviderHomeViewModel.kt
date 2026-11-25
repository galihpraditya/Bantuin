package id.bantuin.app.ui.provider.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.ProviderCandidate
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

data class ProviderHomeUiState(
    val user: UserProfile? = null,
    val provider: ProviderCandidate? = null,
    val specialtyLabel: String = "Mitra Jasa Bantuin",
    val trustResult: TrustResult? = null,
    val pendingCount: Int = 0,
    val todayOrdersCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProviderHomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val metricsRepository: MetricsRepository? = catalogRepository as? MetricsRepository,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderHomeUiState())
    val uiState: StateFlow<ProviderHomeUiState> = _uiState.asStateFlow()

    private var ordersJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        observeBookingCounts()
        loadData()
    }

    private fun observeBookingCounts() {
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            bookingRepository.observeProviderBookings().collect { orders ->
                applyOrderCounts(orders)
            }
        }
    }

    fun loadData(providerId: String? = null) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() } ?: user?.id
            _uiState.update { it.copy(user = user, isLoading = true, errorMessage = null) }

            coroutineScope {
                val providerDeferred = resolvedId?.let { async { catalogRepository.providerProfile(it) } }
                val servicesDeferred = resolvedId?.let { async { catalogRepository.servicesOf(it) } }
                val metricsDeferred = if (resolvedId != null && metricsRepository != null) {
                    async { metricsRepository.trustInput(resolvedId, 30) }
                } else null
                val ordersDeferred = async {
                    bookingRepository.expirePending()
                    bookingRepository.myBookingsProvider()
                }

                val providerRes = providerDeferred?.await()
                val servicesRes = servicesDeferred?.await()
                val metricsRes = metricsDeferred?.await()
                val ordersRes = ordersDeferred.await()

                val provider = (providerRes as? AppResult.Success)?.data
                val firstService = (servicesRes as? AppResult.Success)?.data?.firstOrNull()?.name
                val specialty = when {
                    !firstService.isNullOrBlank() -> firstService
                    !provider?.bio.isNullOrBlank() -> provider!!.bio
                    else -> "Mitra Jasa Bantuin"
                }

                val trustResult = (metricsRes as? AppResult.Success)?.data?.let {
                    trustScoreUseCase.calculate(it)
                }

                val orders = (ordersRes as? AppResult.Success)?.data ?: emptyList()
                applyOrderCounts(orders)

                val err = when {
                    providerRes is AppResult.Error -> providerRes.message
                    ordersRes is AppResult.Error -> ordersRes.message
                    else -> null
                }

                _uiState.update {
                    it.copy(
                        provider = provider,
                        specialtyLabel = specialty,
                        trustResult = trustResult,
                        isLoading = false,
                        errorMessage = err
                    )
                }
            }
        }
    }

    private fun applyOrderCounts(orders: List<BookingItem>) {
        val today = TimeUtils.todayIso()
        val pending = orders.count { it.status == BookingStatus.PENDING }
        val todayOrActive = orders.count {
            it.scheduledDate == today ||
                it.status in setOf(BookingStatus.DIKONFIRMASI, BookingStatus.DALAM_PENGERJAAN)
        }
        _uiState.update {
            it.copy(
                pendingCount = pending,
                todayOrdersCount = todayOrActive
            )
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
}
