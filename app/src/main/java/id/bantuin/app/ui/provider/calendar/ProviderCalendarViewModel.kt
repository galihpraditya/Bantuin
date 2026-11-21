package id.bantuin.app.ui.provider.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderCalendarUiState(
    val providerId: String = "p01",
    val availabilities: List<DayAvailability> = emptyList(),
    val selectedDayOfWeek: Int = 1,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ProviderCalendarViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderCalendarUiState())
    val uiState: StateFlow<ProviderCalendarUiState> = _uiState.asStateFlow()

    init {
        loadAvailability()
    }

    fun loadAvailability(providerId: String = "p01") {
        viewModelScope.launch {
            _uiState.update { it.copy(providerId = providerId, errorMessage = null) }
            when (val res = catalogRepository.availability(providerId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(availabilities = res.data) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun selectDay(dayOfWeek: Int) {
        _uiState.update { it.copy(selectedDayOfWeek = dayOfWeek, saveSuccess = false) }
    }

    fun updateDayMode(dayOfWeek: Int, mode: AvailabilityMode) {
        val list = _uiState.value.availabilities.map { item ->
            if (item.dayOfWeek == dayOfWeek) {
                item.copy(mode = mode)
            } else item
        }
        _uiState.update { it.copy(availabilities = list, saveSuccess = false) }
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

    fun saveChanges() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
            when (val res = catalogRepository.saveAvailability(state.providerId, state.availabilities)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
