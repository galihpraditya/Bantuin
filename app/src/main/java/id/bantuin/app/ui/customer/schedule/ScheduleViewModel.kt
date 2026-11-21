package id.bantuin.app.ui.customer.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.LockInfo
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.model.SlotCheckResult
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.usecase.scheduling.SchedulingConflictDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class ScheduleUiState(
    val provider: ProviderCandidate? = null,
    val service: ServiceItem? = null,
    val distanceKm: Double = 1.8,
    val selectedDateIdx: Int = 0,
    val selectedDate: LocalDate = LocalDate.now(),
    val dynamicDates: List<Triple<String, String, LocalDate>> = emptyList(),
    val providerAvailability: List<DayAvailability> = emptyList(),
    val existingBlocks: List<TimeBlock> = emptyList(),
    val selectedStartMin: Int = 540,
    val slotCheckResult: SlotCheckResult? = null,
    val lockInfo: LockInfo? = null,
    val isLocking: Boolean = false,
    val errorMessage: String? = null
)

class ScheduleViewModel(
    private val catalogRepository: CatalogRepository,
    private val bookingRepository: BookingRepository,
    private val detector: SchedulingConflictDetector = SchedulingConflictDetector()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    fun initialize(
        provider: ProviderCandidate,
        service: ServiceItem,
        distanceKm: Double,
        initialStartMin: Int = 540
    ) {
        val today = LocalDate.now()
        val dynamicDates = (0..6).map { i ->
            val d = today.plusDays(i.toLong())
            val dayName = when (d.dayOfWeek.value) {
                1 -> "Sen"
                2 -> "Sel"
                3 -> "Rab"
                4 -> "Kam"
                5 -> "Jum"
                6 -> "Sab"
                else -> "Min"
            }
            Triple(dayName, "${d.dayOfMonth}", d)
        }

        _uiState.update {
            it.copy(
                provider = provider,
                service = service,
                distanceKm = distanceKm,
                selectedDateIdx = 0,
                selectedDate = today,
                selectedStartMin = initialStartMin,
                dynamicDates = dynamicDates
            )
        }
        loadAvailabilityAndBlocks(provider.providerId, today)
    }

    fun selectDate(idx: Int) {
        val state = _uiState.value
        if (idx in state.dynamicDates.indices) {
            val date = state.dynamicDates[idx].third
            _uiState.update {
                it.copy(
                    selectedDateIdx = idx,
                    selectedDate = date
                )
            }
            val providerId = state.provider?.providerId ?: return
            loadAvailabilityAndBlocks(providerId, date)
        }
    }

    private fun loadAvailabilityAndBlocks(providerId: String, date: LocalDate) {
        viewModelScope.launch {
            val dateIso = date.toString()
            val availRes = catalogRepository.availability(providerId)
            val busyRes = bookingRepository.busyBlocks(providerId, dateIso)

            val avail = if (availRes is AppResult.Success) availRes.data else emptyList()
            val busy = if (busyRes is AppResult.Success) busyRes.data else emptyList()

            _uiState.update {
                it.copy(
                    providerAvailability = avail,
                    existingBlocks = busy
                )
            }
            recomputeSlotCheck()
        }
    }

    fun selectSlot(startMin: Int) {
        _uiState.update { it.copy(selectedStartMin = startMin) }
        recomputeSlotCheck()
    }

    private fun recomputeSlotCheck() {
        val state = _uiState.value
        val service = state.service ?: return
        val dayOfWeekIso = state.selectedDate.dayOfWeek.value
        val dayAvail = state.providerAvailability.find { it.dayOfWeek == dayOfWeekIso } ?: DayAvailability(
            dayOfWeek = dayOfWeekIso,
            mode = AvailabilityMode.ON,
            openMin = 480,
            closeMin = 1020
        )

        val nowMin = if (state.selectedDateIdx == 0) {
            val now = LocalTime.now()
            now.hour * 60 + now.minute
        } else null

        val check = detector.check(
            requestedStartMin = state.selectedStartMin,
            durationMin = service.durationMin,
            day = dayAvail,
            existing = state.existingBlocks,
            nowMin = nowMin
        )

        _uiState.update { it.copy(slotCheckResult = check) }
    }

    fun acquireLockAndProceed(onSuccess: (LockInfo) -> Unit) {
        val state = _uiState.value
        val providerId = state.provider?.providerId ?: return
        val startMin = state.selectedStartMin
        val durationMin = state.service?.durationMin ?: 60

        viewModelScope.launch {
            _uiState.update { it.copy(isLocking = true, errorMessage = null) }
            when (val lockRes = bookingRepository.acquireLock(providerId, startMin, startMin + durationMin)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLocking = false, lockInfo = lockRes.data) }
                    onSuccess(lockRes.data)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLocking = false, errorMessage = lockRes.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
