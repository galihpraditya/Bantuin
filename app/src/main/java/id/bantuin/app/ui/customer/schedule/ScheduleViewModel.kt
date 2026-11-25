package id.bantuin.app.ui.customer.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.util.TimeUtils
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ScheduleUiState(
    val provider: ProviderCandidate? = null,
    val service: ServiceItem? = null,
    val distanceKm: Double = 1.8,
    val selectedDateIdx: Int = 0,
    val selectedDate: LocalDate = LocalDate.now(ZoneId.of(BantuinConfig.TIMEZONE_ID)),
    val monthYearLabel: String = "",
    val dynamicDates: List<Triple<String, String, LocalDate>> = emptyList(),
    val providerAvailability: List<DayAvailability> = emptyList(),
    val existingBlocks: List<TimeBlock> = emptyList(),
    val availableSlots: List<Pair<Int, String>> = emptyList(),
    val slotValidMap: Map<Int, Boolean> = emptyMap(),
    val selectedStartMin: Int = 540,
    val slotCheckResult: SlotCheckResult? = null,
    val lockInfo: LockInfo? = null,
    val isLoading: Boolean = false,
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

    private val wibZone = ZoneId.of(BantuinConfig.TIMEZONE_ID)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))

    fun initialize(
        provider: ProviderCandidate,
        service: ServiceItem,
        distanceKm: Double,
        initialStartMin: Int = 540
    ) {
        val currentState = _uiState.value
        if (currentState.provider?.providerId == provider.providerId &&
            currentState.service?.id == service.id &&
            currentState.dynamicDates.isNotEmpty()
        ) {
            return
        }

        val today = LocalDate.now(wibZone)
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
                monthYearLabel = monthYearFormatter.format(today).replaceFirstChar { ch -> ch.uppercase() },
                selectedStartMin = initialStartMin,
                dynamicDates = dynamicDates,
                errorMessage = null
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
                    selectedDate = date,
                    monthYearLabel = monthYearFormatter.format(date).replaceFirstChar { ch -> ch.uppercase() },
                    errorMessage = null
                )
            }
            val providerId = state.provider?.providerId ?: return
            loadAvailabilityAndBlocks(providerId, date)
        }
    }

    fun refresh() {
        val state = _uiState.value
        val providerId = state.provider?.providerId ?: return
        loadAvailabilityAndBlocks(providerId, state.selectedDate)
    }

    private fun loadAvailabilityAndBlocks(providerId: String, date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val dateIso = date.toString()
                val availDeferred = async { catalogRepository.availability(providerId) }
                val busyDeferred = async { bookingRepository.busyBlocks(providerId, dateIso) }

                val availRes = availDeferred.await()
                val busyRes = busyDeferred.await()

                val err = when {
                    availRes is AppResult.Error -> availRes.message
                    busyRes is AppResult.Error -> busyRes.message
                    else -> null
                }

                val avail = if (availRes is AppResult.Success) availRes.data else emptyList()
                val busy = if (busyRes is AppResult.Success) busyRes.data else emptyList()

                _uiState.update {
                    it.copy(
                        providerAvailability = avail,
                        existingBlocks = busy,
                        isLoading = false,
                        errorMessage = err
                    )
                }
                recomputeSchedule()
            }
        }
    }

    fun selectSlot(startMin: Int) {
        _uiState.update { it.copy(selectedStartMin = startMin, errorMessage = null) }
        recomputeSchedule()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun recomputeSchedule() {
        val state = _uiState.value
        val service = state.service ?: return
        val dayOfWeekIso = state.selectedDate.dayOfWeek.value
        val dayAvail = state.providerAvailability.find { it.dayOfWeek == dayOfWeekIso } ?: DayAvailability(
            dayOfWeek = dayOfWeekIso,
            mode = AvailabilityMode.ON,
            openMin = 480,
            closeMin = 1020
        )

        val slots = when (dayAvail.mode) {
            AvailabilityMode.OFF -> emptyList()
            AvailabilityMode.ALWAYS_ON -> {
                (0 until 1440 step BantuinConfig.SLOT_GRID_MIN)
                    .filter { it + service.durationMin + BantuinConfig.TRAVEL_BUFFER_MIN <= 1440 }
                    .map { min -> min to TimeUtils.formatMinuteOfDay(min) }
            }
            AvailabilityMode.ON -> {
                val start = dayAvail.openMin ?: 480
                val end = (dayAvail.closeMin ?: 1020) - service.durationMin
                if (start <= end) {
                    (start..end step 30).map { min -> min to TimeUtils.formatMinuteOfDay(min) }
                } else {
                    emptyList()
                }
            }
        }

        val nowMin = if (state.selectedDateIdx == 0) {
            val now = LocalTime.now(wibZone)
            now.hour * 60 + now.minute
        } else null

        val validMap = slots.associate { (min, _) ->
            val check = detector.check(
                requestedStartMin = min,
                durationMin = service.durationMin,
                day = dayAvail,
                existing = state.existingBlocks,
                nowMin = nowMin
            )
            min to check.valid
        }

        val effectiveStartMin = if (slots.isNotEmpty() && slots.none { it.first == state.selectedStartMin }) {
            slots.firstOrNull { validMap[it.first] == true }?.first ?: slots.first().first
        } else {
            state.selectedStartMin
        }

        val check = detector.check(
            requestedStartMin = effectiveStartMin,
            durationMin = service.durationMin,
            day = dayAvail,
            existing = state.existingBlocks,
            nowMin = nowMin
        )

        _uiState.update {
            it.copy(
                availableSlots = slots,
                slotValidMap = validMap,
                selectedStartMin = effectiveStartMin,
                slotCheckResult = check
            )
        }
    }

    fun acquireLockAndProceed(onSuccess: (LockInfo) -> Unit) {
        val state = _uiState.value
        val providerId = state.provider?.providerId ?: return
        val check = state.slotCheckResult
        if (check?.valid != true) {
            _uiState.update {
                it.copy(
                    errorMessage = check?.reason?.message
                        ?: "Slot waktu tidak tersedia. Pilih jam lain yang bebas konflik."
                )
            }
            return
        }

        val startMin = state.selectedStartMin
        val durationMin = state.service?.durationMin ?: 60
        val lockEndMin = startMin + durationMin + BantuinConfig.TRAVEL_BUFFER_MIN
        val dateIso = state.selectedDate.toString()

        viewModelScope.launch {
            _uiState.update { it.copy(isLocking = true, errorMessage = null) }
            when (val lockRes = bookingRepository.acquireLock(providerId, dateIso, startMin, lockEndMin)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLocking = false, lockInfo = lockRes.data) }
                    onSuccess(lockRes.data)
                }
                is AppResult.Error -> {
                    val busyRes = bookingRepository.busyBlocks(providerId, dateIso)
                    if (busyRes is AppResult.Success) {
                        _uiState.update { it.copy(existingBlocks = busyRes.data) }
                        recomputeSchedule()
                    }
                    _uiState.update { it.copy(isLocking = false, errorMessage = lockRes.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
