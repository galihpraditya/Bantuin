package id.bantuin.app.ui.provider.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderHomeUiState(
    val user: UserProfile? = null,
    val provider: ProviderCandidate? = null,
    val pendingCount: Int = 0,
    val todayOrdersCount: Int = 0,
    val isOnline: Boolean = true,
    val isLoading: Boolean = false
)

class ProviderHomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderHomeUiState())
    val uiState: StateFlow<ProviderHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        loadData()
    }

    /**
     * Data mitra diambil berdasarkan id akun yang login. Tidak ada fallback ke
     * "p01"/Budi Santoso agar akun mitra lain tidak menampilkan profil orang lain.
     */
    fun loadData(providerId: String? = null) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() } ?: user?.id
            _uiState.update { it.copy(user = user, isLoading = true) }

            val candidatesRes = catalogRepository.candidates(2) // AC category
            val ordersRes = bookingRepository.myBookingsProvider()

            val provider = if (candidatesRes is AppResult.Success && resolvedId != null) {
                candidatesRes.data.find { it.providerId == resolvedId }
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
