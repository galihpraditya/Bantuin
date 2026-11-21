package id.bantuin.app.ui.customer.orderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val bookingId: String = "",
    val booking: BookingItem? = null,
    val rating: Int = 5,
    val reviewComment: String = "",
    val selectedTags: Set<String> = setOf("Tepat Waktu", "Sangat Rapi"),
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

    fun observeBooking(bookingId: String) {
        _uiState.update { it.copy(bookingId = bookingId) }
        viewModelScope.launch {
            bookingRepository.observeBooking(bookingId).collect { item ->
                _uiState.update {
                    it.copy(
                        booking = item,
                        reviewSubmitted = item?.review != null
                    )
                }
            }
        }
    }

    fun setRating(rating: Int) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun setReviewComment(comment: String) {
        _uiState.update { it.copy(reviewComment = comment) }
    }

    fun toggleTag(tag: String) {
        _uiState.update {
            val current = it.selectedTags.toMutableSet()
            if (current.contains(tag)) current.remove(tag) else current.add(tag)
            it.copy(selectedTags = current)
        }
    }

    fun submitReview(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        val booking = state.booking ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReview = true, errorMessage = null) }
            val res = reviewRepository.submitReview(
                bookingId = state.bookingId,
                providerId = booking.providerId,
                rating = state.rating,
                comment = state.reviewComment,
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
        val bookingId = _uiState.value.bookingId
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, errorMessage = null) }
            when (val res = bookingRepository.updateStatus(bookingId, status)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isUpdatingStatus = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isUpdatingStatus = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
