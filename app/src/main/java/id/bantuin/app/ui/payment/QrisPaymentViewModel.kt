package id.bantuin.app.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.BookingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QrisPaymentUiState(
    val bookingId: String = "",
    val booking: BookingItem? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isPaid: Boolean = false,
    val errorMessage: String? = null
)

class QrisPaymentViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrisPaymentUiState())
    val uiState: StateFlow<QrisPaymentUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun loadBooking(bookingId: String) {
        observeJob?.cancel()
        _uiState.update {
            it.copy(
                bookingId = bookingId,
                isLoading = true,
                errorMessage = null
            )
        }
        observeJob = viewModelScope.launch {
            bookingRepository.observeBooking(bookingId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Gagal memuat tagihan QRIS."
                        )
                    }
                }
                .collect { booking ->
                    _uiState.update {
                        it.copy(
                            booking = booking,
                            isLoading = false,
                            isPaid = booking?.paymentStatus == PaymentStatus.PAID,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun confirmSimulatedPayment(onSuccess: () -> Unit = {}) {
        val bookingId = _uiState.value.bookingId
        if (bookingId.isBlank() || _uiState.value.isProcessing || _uiState.value.isPaid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            when (val res = bookingRepository.confirmQrisPayment(bookingId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            isPaid = true,
                            booking = it.booking?.copy(paymentStatus = PaymentStatus.PAID)
                        )
                    }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = res.message
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
