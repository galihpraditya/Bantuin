package id.bantuin.app.ui.provider.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderCalendarUiState(
    val providerId: String = "",
    val availabilities: List<DayAvailability> = emptyList(),
    val upcomingBookings: List<BookingItem> = emptyList(),
    val upcomingBookingsLoaded: Boolean = false,
    val selectedDayOfWeek: Int = 1,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ProviderCalendarViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository? = catalogRepository as? BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderCalendarUiState())
    val uiState: StateFlow<ProviderCalendarUiState> = _uiState.asStateFlow()

    private var bookingsJob: Job? = null

    init {
        loadAvailability()
    }

    fun loadAvailability(providerId: String? = null) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            val resolvedId = providerId?.takeIf { it.isNotBlank() } ?: user?.id
            if (resolvedId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Akun mitra tidak ditemukan") }
                return@launch
            }
            _uiState.update { it.copy(providerId = resolvedId, isLoading = true, errorMessage = null) }

            observeBookingsIfAvailable()

            when (val res = catalogRepository.availability(resolvedId)) {
                is AppResult.Success -> {
                    val completeWeek = (1..7).map { dow ->
                        res.data.find { it.dayOfWeek == dow } ?: DayAvailability(
                            dayOfWeek = dow,
                            mode = AvailabilityMode.ON,
                            openMin = 480,
                            closeMin = 1020
                        )
                    }
                    _uiState.update {
                        it.copy(
                            availabilities = completeWeek,
                            isLoading = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    private fun observeBookingsIfAvailable() {
        val repo = bookingRepository ?: run {
            _uiState.update { it.copy(upcomingBookingsLoaded = true) }
            return
        }
        bookingsJob?.cancel()
        bookingsJob = viewModelScope.launch {
            repo.expirePending()
            val res = repo.myBookingsProvider()
            if (res is AppResult.Success) {
                val active = res.data.filter {
                    it.status in setOf(
                        BookingStatus.PENDING,
                        BookingStatus.DIKONFIRMASI,
                        BookingStatus.DALAM_PENGERJAAN
                    )
                }
                _uiState.update {
                    it.copy(
                        upcomingBookings = active,
                        upcomingBookingsLoaded = true
                    )
                }
            } else {
                _uiState.update { it.copy(upcomingBookingsLoaded = true) }
            }

            repo.observeProviderBookings().collect { list ->
                val active = list.filter {
                    it.status in setOf(
                        BookingStatus.PENDING,
                        BookingStatus.DIKONFIRMASI,
                        BookingStatus.DALAM_PENGERJAAN
                    )
                }
                _uiState.update {
                    it.copy(
                        upcomingBookings = active,
                        upcomingBookingsLoaded = true
                    )
                }
            }
        }
    }

    fun selectDay(dayOfWeek: Int) {
        _uiState.update { it.copy(selectedDayOfWeek = dayOfWeek, saveSuccess = false) }
    }

    fun updateDayMode(dayOfWeek: Int, mode: AvailabilityMode) {
        val list = _uiState.value.availabilities.map { item ->
            if (item.dayOfWeek == dayOfWeek) {
                item.copy(
                    mode = mode,
                    openMin = if (mode == AvailabilityMode.ON) (item.openMin ?: 480) else null,
                    closeMin = if (mode == AvailabilityMode.ON) (item.closeMin ?: 1020) else null
                )
            } else item
        }
        _uiState.update { it.copy(availabilities = list, saveSuccess = false, errorMessage = null) }
    }

    fun updateOperatingHours(dayOfWeek: Int, openMin: Int, closeMin: Int) {
        if (closeMin <= openMin) {
            _uiState.update { it.copy(errorMessage = "Jam buka harus lebih awal dari jam tutup") }
            return
        }
        val list = _uiState.value.availabilities.map { item ->
            if (item.dayOfWeek == dayOfWeek) {
                item.copy(openMin = openMin, closeMin = closeMin)
            } else item
        }
        _uiState.update { it.copy(availabilities = list, errorMessage = null, saveSuccess = false) }
    }

    fun updateHourFieldUnchecked(dayOfWeek: Int, openMin: Int?, closeMin: Int?) {
        val list = _uiState.value.availabilities.map { item ->
            if (item.dayOfWeek == dayOfWeek) {
                item.copy(
                    openMin = openMin ?: item.openMin,
                    closeMin = closeMin ?: item.closeMin
                )
            } else item
        }
        _uiState.update { it.copy(availabilities = list, errorMessage = null, saveSuccess = false) }
    }

    fun saveChanges() {
        saveDays(_uiState.value.availabilities)
    }

    fun saveDays(days: List<DayAvailability>) {
        val providerId = _uiState.value.providerId
        if (providerId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Akun mitra tidak ditemukan") }
            return
        }
        val hasInvalidHours = days.any {
            it.mode == AvailabilityMode.ON && ((it.openMin ?: 0) >= (it.closeMin ?: 0))
        }
        if (hasInvalidHours) {
            _uiState.update {
                it.copy(errorMessage = "Jam buka harus lebih awal dari jam tutup untuk mode Jam Kerja")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
            when (val res = catalogRepository.saveAvailability(providerId, days)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true, availabilities = days) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
