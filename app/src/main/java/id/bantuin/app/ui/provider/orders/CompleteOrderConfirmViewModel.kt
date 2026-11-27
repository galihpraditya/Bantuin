package id.bantuin.app.ui.provider.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.BookingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompleteOrderConfirmUiState(
    val bookingId: String = "",
    val booking: BookingItem? = null,
    val completionNote: String = "",
    val cashCollectedConfirmed: Boolean = false,
    val canConfirm: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class CompleteOrderConfirmViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompleteOrderConfirmUiState())
    val uiState: StateFlow<CompleteOrderConfirmUiState> = _uiState.asStateFlow()

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
                            errorMessage = e.message ?: "Gagal memuat detail pesanan."
                        )
                    }
                }
                .collect { booking ->
                    _uiState.update { state ->
                        state.copy(
                            booking = booking,
                            isLoading = false,
                            isCompleted = state.isCompleted || booking?.status == BookingStatus.SELESAI,
                            canConfirm = evaluateCanConfirm(booking, state.cashCollectedConfirmed)
                        )
                    }
                }
        }
    }

    fun onCompletionNoteChange(note: String) {
        _uiState.update { it.copy(completionNote = note.take(300)) }
    }

    fun setCashCollectedConfirmed(confirmed: Boolean) {
        _uiState.update { state ->
            state.copy(
                cashCollectedConfirmed = confirmed,
                canConfirm = evaluateCanConfirm(state.booking, confirmed),
                errorMessage = null
            )
        }
    }

    fun confirmCompletion(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        val booking = state.booking
        if (booking == null) {
            _uiState.update { it.copy(errorMessage = "Data pesanan belum tersedia.") }
            return
        }
        if (booking.status != BookingStatus.DALAM_PENGERJAAN) {
            _uiState.update { it.copy(errorMessage = "Pesanan harus berstatus Dalam Pengerjaan sebelum diselesaikan.") }
            return
        }
        if (booking.paymentMethod == PaymentMethod.CASH && !state.cashCollectedConfirmed) {
            _uiState.update {
                it.copy(errorMessage = "Centang konfirmasi penerimaan uang tunai terlebih dahulu.")
            }
            return
        }
        if (booking.paymentMethod == PaymentMethod.QRIS && booking.paymentStatus != PaymentStatus.PAID) {
            _uiState.update {
                it.copy(errorMessage = "Pembayaran QRIS pelanggan belum berstatus Lunas.")
            }
            return
        }
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val note = state.completionNote.trim().ifBlank { null }
            when (val res = bookingRepository.completeBooking(booking.id, note, state.cashCollectedConfirmed)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isCompleted = true,
                            errorMessage = null
                        )
                    }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = res.message
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    private fun evaluateCanConfirm(booking: BookingItem?, cashConfirmed: Boolean): Boolean {
        if (booking == null || booking.status != BookingStatus.DALAM_PENGERJAAN) return false
        return when (booking.paymentMethod) {
            PaymentMethod.CASH -> cashConfirmed
            PaymentMethod.QRIS -> booking.paymentStatus == PaymentStatus.PAID
        }
    }
}
