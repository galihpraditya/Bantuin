package id.bantuin.app.ui.customer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.repository.BookingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerOrdersUiState(
    val selectedTab: Int = 0,
    val orders: List<BookingItem> = emptyList(),
    val activeOrders: List<BookingItem> = emptyList(),
    val historyOrders: List<BookingItem> = emptyList(),
    val hasSyncedPendingExpiry: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CustomerOrdersViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerOrdersUiState())
    val uiState: StateFlow<CustomerOrdersUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeBookings()
        loadOrders()
    }

    private fun observeBookings() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            bookingRepository.observeCustomerBookings()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Gagal memuat daftar pesanan.")
                    }
                }
                .collect { bookings ->
                    applyOrders(bookings)
                }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            bookingRepository.expirePending()
            _uiState.update { it.copy(hasSyncedPendingExpiry = true) }

            when (val res = bookingRepository.myBookingsCustomer()) {
                is AppResult.Success -> {
                    applyOrders(res.data)
                    _uiState.update { it.copy(isLoading = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    private fun applyOrders(all: List<BookingItem>) {
        val active = all.filter {
            it.status in listOf(
                BookingStatus.PENDING,
                BookingStatus.DIKONFIRMASI,
                BookingStatus.DALAM_PENGERJAAN
            )
        }
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
                activeOrders = active,
                historyOrders = history
            )
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
