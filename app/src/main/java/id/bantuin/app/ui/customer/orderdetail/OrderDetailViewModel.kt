package id.bantuin.app.ui.customer.orderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.dispatcher.AppDispatchers
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingEvent
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.CompletionStatus
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.ReviewRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class OrderDetailUiState(
    val bookingId: String = "",
    val booking: BookingItem? = null,
    val events: List<BookingEvent> = emptyList(),
    val completion: CompletionStatus? = null,
    val isLoadingBooking: Boolean = true,
    val bookingNotFound: Boolean = false,
    val reviewSubmitted: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val rating: Int = 5,
    val reviewComment: String = "",
    val selectedTags: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class OrderDetailViewModel(
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val requests = Mutex()
    private var loadJob: Job? = null
    private var draftEdited = false

    fun observeBooking(bookingId: String, background: Boolean = false) {
        if (_uiState.value.bookingId != bookingId) {
            loadJob?.cancel()
            draftEdited = false
            _uiState.value = OrderDetailUiState(bookingId = bookingId)
        }
        if (loadJob?.isActive == true || _uiState.value.isSaving) return
        loadJob = viewModelScope.launch {
            requests.withLock { load(bookingId, background) }
            bookingRepository.observeBooking(bookingId).collect { item ->
                _uiState.update {
                    it.copy(
                        booking = item,
                        bookingNotFound = item == null,
                        isLoadingBooking = false,
                        reviewSubmitted = item?.review != null
                    )
                }
            }
        }
    }

    private suspend fun load(bookingId: String, background: Boolean = false) {
        val (detail, events) = withContext(dispatchers.io) {
            bookingRepository.bookingDetails(bookingId) to bookingRepository.bookingEvents(bookingId)
        }
        if (_uiState.value.bookingId != bookingId) return
        when (detail) {
            is AppResult.Success -> {
                val booking = detail.data
                val review = booking?.review
                val timeline = (events as? AppResult.Success)?.data
                _uiState.update { state ->
                    val updated = state.copy(
                        booking = booking,
                        bookingNotFound = booking == null,
                        isLoadingBooking = false,
                        events = timeline ?: state.events,
                        isLoading = false,
                        reviewSubmitted = review != null,
                        errorMessage = when {
                            booking == null -> "Pesanan tidak ditemukan"
                            background -> state.errorMessage
                            else -> (events as? AppResult.Error)?.message
                        }
                    )
                    if (draftEdited) updated else updated.copy(
                        completion = booking?.outcome?.status ?: booking?.status?.completionOptions()?.singleOrNull(),
                        rating = review?.rating ?: 5,
                        reviewComment = review?.comment.orEmpty(),
                        selectedTags = review?.tags.orEmpty().toSet()
                    )
                }
            }
            is AppResult.Error -> _uiState.update {
                it.copy(isLoading = false, isLoadingBooking = false,
                    errorMessage = if (background && it.booking != null) it.errorMessage else detail.message)
            }
            AppResult.Loading -> Unit
        }
    }

    fun setCompletion(status: CompletionStatus) = edit { it.copy(completion = status) }
    fun setRating(rating: Int) = edit { it.copy(rating = rating.coerceIn(1, 5)) }
    fun setReviewComment(comment: String) = edit { it.copy(reviewComment = comment.take(300)) }
    fun toggleTag(tag: String) = edit {
        it.copy(selectedTags = if (tag in it.selectedTags) it.selectedTags - tag else it.selectedTags + tag)
    }

    private fun edit(transform: (OrderDetailUiState) -> OrderDetailUiState) {
        if (_uiState.value.isSaving) return
        draftEdited = true
        _uiState.update { transform(it).copy(successMessage = null) }
    }

    fun submitReview() {
        val state = _uiState.value
        val booking = state.booking ?: return
        val completion = state.completion?.takeIf { it in booking.status.completionOptions() } ?: return
        mutate("Konfirmasi dan ulasan tersimpan") {
            reviewRepository.confirmCompletion(
                booking.id, booking.providerId, completion, state.rating,
                state.reviewComment, state.selectedTags.toList()
            )
        }
    }

    fun submitReview(onSuccess: () -> Unit) = submitReview()

    fun deleteReview() = mutate("Ulasan dihapus") { reviewRepository.deleteReview(it) }
    fun deleteConfirmation() = mutate("Konfirmasi dihapus") { reviewRepository.deleteTransactionOutcome(it) }

    fun updateStatus(status: BookingStatus) {
        val state = _uiState.value
        val current = state.booking?.status
        if (current == null) {
            _uiState.update { it.copy(errorMessage = "Data pesanan belum tersedia") }
            return
        }
        if (!current.canTransitionTo(status)) {
            _uiState.update {
                it.copy(errorMessage = "Status ${current.displayName} tidak bisa diubah menjadi ${status.displayName}")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, errorMessage = null) }
            when (val result = withContext(dispatchers.io) { bookingRepository.updateStatus(state.bookingId, status) }) {
                is AppResult.Success -> _uiState.update { it.copy(isUpdatingStatus = false) }
                is AppResult.Error -> _uiState.update { it.copy(isUpdatingStatus = false, errorMessage = result.message) }
                AppResult.Loading -> _uiState.update { it.copy(isUpdatingStatus = false) }
            }
        }
    }

    private fun mutate(message: String, action: suspend (String) -> AppResult<Unit>) {
        val state = _uiState.value
        if (state.isSaving || state.booking == null) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            requests.withLock {
                when (val result = withContext(dispatchers.io) { action(state.bookingId) }) {
                    is AppResult.Success -> {
                        draftEdited = false
                        load(state.bookingId)
                        _uiState.update { it.copy(isSaving = false, successMessage = message) }
                    }
                    is AppResult.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                    AppResult.Loading -> _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }
}

internal fun BookingStatus.completionOptions(): List<CompletionStatus> = when (this) {
    BookingStatus.PENDING, BookingStatus.DIKONFIRMASI,
    BookingStatus.DIBATALKAN_PELANGGAN, BookingStatus.DIBATALKAN_MITRA -> listOf(CompletionStatus.DIBATALKAN)
    BookingStatus.DALAM_PENGERJAAN -> CompletionStatus.entries
    BookingStatus.SELESAI -> listOf(CompletionStatus.SELESAI_TEPAT_WAKTU, CompletionStatus.TERLAMBAT)
    else -> emptyList()
}
