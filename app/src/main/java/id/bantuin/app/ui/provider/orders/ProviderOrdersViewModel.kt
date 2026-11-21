package id.bantuin.app.ui.provider.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderOrdersUiState(
    val selectedTab: Int = 0,
    val orders: List<BookingItem> = emptyList(),
    val pendingOrders: List<BookingItem> = emptyList(),
    val confirmedOrders: List<BookingItem> = emptyList(),
    val inProgressOrders: List<BookingItem> = emptyList(),
    val historyOrders: List<BookingItem> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val errorMessage: String? = null
)

class ProviderOrdersViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderOrdersUiState())
    val uiState: StateFlow<ProviderOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = bookingRepository.myBookingsProvider()) {
                is AppResult.Success -> {
                    val all = res.data
                    val pending = all.filter { it.status == BookingStatus.PENDING }
                    val confirmed = all.filter { it.status == BookingStatus.DIKONFIRMASI }
                    val inProgress = all.filter { it.status == BookingStatus.DALAM_PENGERJAAN }
                    val history = all.filter {
                        it.status in listOf(
                            BookingStatus.SELESAI,
                            BookingStatus.DITOLAK,
                            BookingStatus.DIBATALKAN_PELANGGAN,
                            BookingStatus.DIBATALKAN_MITRA,
                            BookingStatus.KEDALUWARSA
                        )
                    }
                    _uiState.update {
                        it.copy(
                            orders = all,
                            pendingOrders = pending,
                            confirmedOrders = confirmed,
                            inProgressOrders = inProgress,
                            historyOrders = history,
                            isLoading = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateStatus(bookingId: String, newStatus: BookingStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, errorMessage = null) }
            when (val res = bookingRepository.updateStatus(bookingId, newStatus)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isUpdatingStatus = false) }
                    loadOrders()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isUpdatingStatus = false, errorMessage = res.message) }
                }
            }
        }
    }
}
