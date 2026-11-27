package id.bantuin.app.ui.customer.estimate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.CostEstimate
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.model.VoucherResult
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.usecase.pricing.CostEstimatorUseCase
import id.bantuin.app.domain.usecase.voucher.VoucherEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstimateUiState(
    val categoryId: Int = 2,
    val provider: ProviderCandidate? = null,
    val service: ServiceItem? = null,
    val customerLocation: GeoPoint = id.bantuin.app.core.config.BantuinConfig.MALANG_CENTER,
    val customerAddress: String = "",
    val scheduledDateIso: String = "",
    val scheduledStartMin: Int = 540,
    val scheduledDurationMin: Int = 90,
    val distanceKm: Double = 1.8,
    val complexity: Complexity = Complexity.SEDANG,
    val isUrgent: Boolean = false,
    val notes: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val costEstimate: CostEstimate? = null,
    val isSubmitting: Boolean = false,
    val createdBookingId: String? = null,
    val errorMessage: String? = null,
    // ── SCALE: Voucher Engine ─────────────────────────────────────
    val voucherCode: String = "",
    val voucherResult: VoucherResult = VoucherResult.Empty,
    val isValidatingVoucher: Boolean = false
)

class EstimateViewModel(
    private val bookingRepository: BookingRepository,
    private val costEstimator: CostEstimatorUseCase = CostEstimatorUseCase(),
    private val voucherEngine: VoucherEngine = VoucherEngine()
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
        distanceKm: Double,
        initialComplexity: Complexity = Complexity.SEDANG,
        initialUrgent: Boolean = false,
        initialNotes: String = "",
        initialPaymentMethod: PaymentMethod = PaymentMethod.CASH
    ) {
        val current = _uiState.value
        if (current.provider?.providerId == provider.providerId &&
            current.service?.id == service.id &&
            current.scheduledDateIso == scheduledDateIso &&
            current.scheduledStartMin == scheduledStartMin &&
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
                distanceKm = distanceKm,
                complexity = initialComplexity,
                isUrgent = initialUrgent,
                notes = initialNotes,
                paymentMethod = initialPaymentMethod,
                costEstimate = initialEstimate,
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
        revalidateVoucherIfActive(newEstimate.total)
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
        revalidateVoucherIfActive(newEstimate.total)
    }

    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes.take(500)) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method, errorMessage = null) }
    }

    fun onVoucherCodeChanged(code: String) {
        _uiState.update { it.copy(voucherCode = code, voucherResult = VoucherResult.Empty) }
    }

    fun applyVoucher() {
        val state = _uiState.value
        val code = state.voucherCode.trim()
        val orderTotal = state.costEstimate?.total ?: return
        _uiState.update { it.copy(isValidatingVoucher = true) }
        val result = voucherEngine.apply(code, orderTotal)
        _uiState.update { it.copy(voucherResult = result, isValidatingVoucher = false) }
    }

    fun clearVoucher() {
        _uiState.update {
            it.copy(
                voucherCode = "",
                voucherResult = VoucherResult.Empty
            )
        }
    }

    fun availableVouchers() = voucherEngine.getAvailableVouchers()

    private fun revalidateVoucherIfActive(newTotal: Int) {
        val state = _uiState.value
        if (state.voucherCode.isBlank()) return
        val result = voucherEngine.apply(state.voucherCode, newTotal)
        _uiState.update { it.copy(voucherResult = result) }
    }

    val finalTotal: Int
        get() {
            val state = _uiState.value
            val baseTotal = state.costEstimate?.total ?: 0
            return when (val result = state.voucherResult) {
                is VoucherResult.Valid -> result.finalTotal
                else -> baseTotal
            }
        }

    fun confirmOrder(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        val incomplete = state.provider == null || state.service == null || state.costEstimate == null
        if (incomplete || state.scheduledDateIso.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Data order tidak lengkap. Kembali dan pilih layanan serta jadwal lagi.")
            }
            return
        }
        val provider = state.provider!!
        val service = state.service!!
        val estimate = state.costEstimate!!
        val effectiveTotal = finalTotal
        val effectiveEstimate = if (effectiveTotal != estimate.total) {
            estimate.copy(total = effectiveTotal)
        } else estimate

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
            costEstimate = effectiveEstimate,
            paymentMethod = state.paymentMethod
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val res = bookingRepository.createBooking(draft)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            createdBookingId = res.data
                        )
                    }
                    onSuccess(res.data)
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = res.message)
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
