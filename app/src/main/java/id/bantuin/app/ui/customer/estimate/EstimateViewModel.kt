package id.bantuin.app.ui.customer.estimate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.CostEstimate
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.LockInfo
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.usecase.pricing.CostEstimatorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstimateUiState(
    val categoryId: Int = 2,
    val provider: ProviderCandidate? = null,
    val service: ServiceItem? = null,
    val customerLocation: GeoPoint = GeoPoint(-7.9797, 112.6304),
    val customerAddress: String = "",
    val scheduledDateIso: String = "",
    val scheduledStartMin: Int = 540,
    val scheduledDurationMin: Int = 90,
    val lockInfo: LockInfo? = null,
    val distanceKm: Double = 1.8,
    val complexity: Complexity = Complexity.SEDANG,
    val isUrgent: Boolean = false,
    val notes: String = "",
    val costEstimate: CostEstimate? = null,
    val isLockExpired: Boolean = false,
    val isSubmitting: Boolean = false,
    val createdBookingId: String? = null,
    val errorMessage: String? = null
)

class EstimateViewModel(
    private val bookingRepository: BookingRepository,
    private val costEstimator: CostEstimatorUseCase = CostEstimatorUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstimateUiState())
    val uiState: StateFlow<EstimateUiState> = _uiState.asStateFlow()

    fun initialize(
        categoryId: Int,
        provider: ProviderCandidate,
        service: ServiceItem,
        customerLocation: GeoPoint,
        customerAddress: String,
        scheduledDateIso: String,
        scheduledStartMin: Int,
        scheduledDurationMin: Int,
        lockInfo: LockInfo?,
        distanceKm: Double,
        initialComplexity: Complexity = Complexity.SEDANG,
        initialUrgent: Boolean = false,
        initialNotes: String = ""
    ) {
        val current = _uiState.value
        if (current.provider?.providerId == provider.providerId &&
            current.service?.id == service.id &&
            current.scheduledDateIso == scheduledDateIso &&
            current.scheduledStartMin == scheduledStartMin &&
            current.lockInfo?.lockId == lockInfo?.lockId &&
            current.costEstimate != null
        ) {
            return
        }

        val initialEstimate = costEstimator(
            baseFare = service.baseFare,
            distanceKm = distanceKm,
            complexity = initialComplexity,
            startMinOfDay = scheduledStartMin,
            isUrgent = initialUrgent
        )

        _uiState.update {
            it.copy(
                categoryId = categoryId,
                provider = provider,
                service = service,
                customerLocation = customerLocation,
                customerAddress = customerAddress,
                scheduledDateIso = scheduledDateIso,
                scheduledStartMin = scheduledStartMin,
                scheduledDurationMin = scheduledDurationMin,
                lockInfo = lockInfo,
                distanceKm = distanceKm,
                complexity = initialComplexity,
                isUrgent = initialUrgent,
                notes = initialNotes,
                costEstimate = initialEstimate,
                isLockExpired = false,
                errorMessage = null
            )
        }
    }

    fun setComplexity(complexity: Complexity) {
        val state = _uiState.value
        val service = state.service ?: return
        val newEstimate = costEstimator(
            baseFare = service.baseFare,
            distanceKm = state.distanceKm,
            complexity = complexity,
            startMinOfDay = state.scheduledStartMin,
            isUrgent = state.isUrgent
        )
        _uiState.update {
            it.copy(
                complexity = complexity,
                costEstimate = newEstimate,
                errorMessage = null
            )
        }
    }

    fun setUrgent(isUrgent: Boolean) {
        val state = _uiState.value
        val service = state.service ?: return
        val newEstimate = costEstimator(
            baseFare = service.baseFare,
            distanceKm = state.distanceKm,
            complexity = state.complexity,
            startMinOfDay = state.scheduledStartMin,
            isUrgent = isUrgent
        )
        _uiState.update {
            it.copy(
                isUrgent = isUrgent,
                costEstimate = newEstimate,
                errorMessage = null
            )
        }
    }

    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes.take(500)) }
    }

    fun onLockExpired() {
        _uiState.update {
            it.copy(
                isLockExpired = true,
                errorMessage = "Batas waktu kunci slot 10 menit telah berakhir. Silakan pilih jadwal ulang."
            )
        }
    }

    fun releaseLock(onDone: () -> Unit = {}) {
        val lockId = _uiState.value.lockInfo?.lockId
        viewModelScope.launch {
            if (!lockId.isNullOrBlank()) {
                bookingRepository.releaseLock(lockId)
            }
            _uiState.update { it.copy(lockInfo = null) }
            onDone()
        }
    }

    fun confirmOrder(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.isLockExpired) {
            _uiState.update {
                it.copy(errorMessage = "Kunci slot telah berakhir. Silakan kembali dan pilih jadwal ulang.")
            }
            return
        }
        val provider = state.provider ?: return
        val service = state.service ?: return
        val estimate = state.costEstimate ?: return

        val draft = OrderDraft(
            categoryId = state.categoryId,
            provider = provider,
            service = service,
            dateIso = state.scheduledDateIso,
            startMin = state.scheduledStartMin,
            durationMin = state.scheduledDurationMin,
            complexity = state.complexity,
            isUrgent = state.isUrgent,
            notes = state.notes.trim(),
            customerLocation = state.customerLocation,
            customerAddress = state.customerAddress,
            distanceKm = state.distanceKm,
            costEstimate = estimate,
            lockInfo = state.lockInfo
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val res = bookingRepository.createBooking(draft)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            createdBookingId = res.data,
                            lockInfo = null
                        )
                    }
                    onSuccess(res.data)
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
}
