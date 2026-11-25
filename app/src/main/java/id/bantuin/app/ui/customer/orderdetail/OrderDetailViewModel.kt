package id.bantuin.app.ui.customer.orderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingEvent
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.ReviewRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val bookingId: String = "",
    val booking: BookingItem? = null,
    val events: List<BookingEvent> = emptyList(),
    val isLoading: Boolean = false,
    val rating: Int = 5,
    val reviewComment: String = "",
    val selectedTags: Set<String> = setOf("Tepat Waktu", "Pekerjaan Rapi"),
    val isSubmittingReview: Boolean = false,
    val reviewSubmitted: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val errorMessage: String? = null
)

class OrderDetailViewModel(
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun observeBooking(bookingId: String) {
        observeJob?.cancel()
        _uiState.update { it.copy(bookingId = bookingId, isLoading = true, errorMessage = null) }
        observeJob = viewModelScope.launch {
            bookingRepository.expirePending()
            bookingRepository.observeBooking(bookingId)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Gagal memuat detail pesanan.")
                    }
                }
                .collect { item ->
                    val eventsRes = bookingRepository.events(bookingId)
                    val events = if (eventsRes is AppResult.Success) eventsRes.data else emptyList()
                    _uiState.update {
                        it.copy(
                            booking = item,
                            events = events,
                            isLoading = false,
                            reviewSubmitted = item?.review != null
                        )
                    }
                }
        }
    }

    fun expireIfOverdue() {
        val bookingId = _uiState.value.bookingId
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            bookingRepository.expirePending()
            val eventsRes = bookingRepository.events(bookingId)
            if (eventsRes is AppResult.Success) {
                _uiState.update { it.copy(events = eventsRes.data) }
            }
        }
    }

    fun setRating(rating: Int) {
        _uiState.update { it.copy(rating = rating.coerceIn(1, 5), errorMessage = null) }
    }

    fun setReviewComment(comment: String) {
        _uiState.update { it.copy(reviewComment = comment, errorMessage = null) }
    }

    fun toggleTag(tag: String) {
        _uiState.update {
            val current = it.selectedTags.toMutableSet()
            if (current.contains(tag)) current.remove(tag) else current.add(tag)
            it.copy(selectedTags = current)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submitReview(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        val booking = state.booking ?: return
        val trimmedComment = state.reviewComment.trim()
        if (state.rating !in 1..5) {
            _uiState.update { it.copy(errorMessage = "Pilih rating antara 1 hingga 5 bintang.") }
            return
        }
        if (trimmedComment.length > 300) {
            _uiState.update { it.copy(errorMessage = "Komentar ulasan maksimal 300 karakter.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReview = true, errorMessage = null) }
            val res = reviewRepository.submitReview(
                bookingId = state.bookingId,
                providerId = booking.providerId,
                rating = state.rating,
                comment = trimmedComment.ifBlank { null },
                tags = state.selectedTags.toList()
            )
            when (res) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmittingReview = false, reviewSubmitted = true) }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSubmittingReview = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun updateStatus(status: BookingStatus) {
        if (status != BookingStatus.DIBATALKAN_PELANGGAN) {
            _uiState.update {
                it.copy(errorMessage = "Hanya mitra yang dapat menyelesaikan atau memperbarui status pengerjaan.")
            }
            return
        }
        val bookingId = _uiState.value.bookingId
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, errorMessage = null) }
            when (val res = bookingRepository.updateStatus(bookingId, status)) {
                is AppResult.Success -> {
                    val eventsRes = bookingRepository.events(bookingId)
                    val events = if (eventsRes is AppResult.Success) eventsRes.data else _uiState.value.events
                    _uiState.update { it.copy(isUpdatingStatus = false, events = events) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isUpdatingStatus = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
