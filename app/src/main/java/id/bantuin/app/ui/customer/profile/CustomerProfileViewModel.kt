package id.bantuin.app.ui.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerProfileUiState(
    val user: UserProfile? = null,
    val nameInput: String = "",
    val phoneInput: String = "",
    val addressInput: String = "",
    val locationInput: GeoPoint = BantuinConfig.MALANG_CENTER,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val pendingActionCount: Int = 0,
    val totalSpend: Int = 0,
    val isEditSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isLocationPickerVisible: Boolean = false,
    val isDetectingGps: Boolean = false,
    val gpsError: String? = null,
    val errorMessage: String? = null
)

class CustomerProfileViewModel(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerProfileUiState())
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    private var hasUnsavedEdits = false

    init {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { state ->
                    if (user == null) {
                        state.copy(user = null, isLoading = false)
                    } else if (!hasUnsavedEdits) {
                        state.copy(
                            user = user,
                            nameInput = user.fullName,
                            phoneInput = user.phone,
                            addressInput = user.defaultAddress ?: state.addressInput.ifBlank { "Malang, Jawa Timur" },
                            locationInput = user.defaultLocation ?: state.locationInput,
                            isLoading = false
                        )
                    } else {
                        state.copy(user = user, isLoading = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            bookingRepository.observeCustomerBookings().collect { bookings ->
                val active = bookings.count { it.status.isActive }
                val completedBookings = bookings.filter { it.status == BookingStatus.SELESAI }
                val completed = completedBookings.size
                val pendingAction = bookings.count {
                    it.paymentStatus == PaymentStatus.WAITING_PAYMENT ||
                        (it.status == BookingStatus.SELESAI && it.review == null)
                }
                val totalSpend = completedBookings.sumOf { it.totalCost }
                _uiState.update {
                    it.copy(
                        activeCount = active,
                        completedCount = completed,
                        pendingActionCount = pendingAction,
                        totalSpend = totalSpend
                    )
                }
            }
        }
    }

    fun openEditSheet() {
        val user = _uiState.value.user
        hasUnsavedEdits = false
        _uiState.update { state ->
            state.copy(
                nameInput = user?.fullName ?: state.nameInput,
                phoneInput = user?.phone ?: state.phoneInput,
                addressInput = user?.defaultAddress ?: state.addressInput,
                locationInput = user?.defaultLocation ?: state.locationInput,
                isEditSheetOpen = true,
                errorMessage = null
            )
        }
    }

    fun closeEditSheet() {
        val user = _uiState.value.user
        hasUnsavedEdits = false
        _uiState.update { state ->
            state.copy(
                nameInput = user?.fullName ?: state.nameInput,
                phoneInput = user?.phone ?: state.phoneInput,
                addressInput = user?.defaultAddress ?: state.addressInput,
                locationInput = user?.defaultLocation ?: state.locationInput,
                isEditSheetOpen = false,
                errorMessage = null
            )
        }
    }

    fun onNameChange(value: String) {
        hasUnsavedEdits = true
        _uiState.update { it.copy(nameInput = value, isSaved = false, errorMessage = null) }
    }

    fun onPhoneChange(value: String) {
        hasUnsavedEdits = true
        _uiState.update { it.copy(phoneInput = value, isSaved = false, errorMessage = null) }
    }

    fun onAddressChange(value: String) {
        hasUnsavedEdits = true
        _uiState.update { it.copy(addressInput = value, isSaved = false, errorMessage = null) }
    }

    fun onLocationPicked(address: String, location: GeoPoint) {
        hasUnsavedEdits = true
        _uiState.update {
            it.copy(
                addressInput = address,
                locationInput = location,
                isLocationPickerVisible = false,
                isDetectingGps = false,
                gpsError = null,
                isSaved = false,
                errorMessage = null
            )
        }
    }

    fun savePickedLocationImmediately(address: String, location: GeoPoint) {
        onLocationPicked(address, location)
        saveCustomerProfile()
    }

    fun setLocationPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(isLocationPickerVisible = visible, gpsError = null) }
    }

    fun setDetectingGps(detecting: Boolean) {
        _uiState.update { it.copy(isDetectingGps = detecting, gpsError = null) }
    }

    fun setGpsError(error: String?) {
        _uiState.update { it.copy(isDetectingGps = false, gpsError = error) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(isSaved = false, errorMessage = null) }
    }

    fun saveCustomerProfile() {
        val state = _uiState.value
        val trimmedName = state.nameInput.trim()
        val normalizedPhone = state.phoneInput.trim().replace(" ", "").replace("-", "")
        val trimmedAddress = state.addressInput.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama lengkap tidak boleh kosong.") }
            return
        }
        if (!INDONESIA_PHONE_REGEX.matches(normalizedPhone)) {
            _uiState.update {
                it.copy(errorMessage = "Nomor telepon Indonesia tidak valid (contoh: 081234567890).")
            }
            return
        }
        if (trimmedAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Alamat layanan utama wajib diisi.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSaved = false, errorMessage = null) }
            when (
                val res = authRepository.updateCustomerProfile(
                    fullName = trimmedName,
                    phone = normalizedPhone,
                    defaultAddress = trimmedAddress,
                    defaultLocation = state.locationInput
                )
            ) {
                is AppResult.Success -> {
                    hasUnsavedEdits = false
                    _uiState.update {
                        it.copy(
                            user = res.data,
                            nameInput = res.data.fullName,
                            phoneInput = res.data.phone,
                            addressInput = res.data.defaultAddress ?: trimmedAddress,
                            locationInput = res.data.defaultLocation ?: state.locationInput,
                            isEditSheetOpen = false,
                            isSaving = false,
                            isSaved = true
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    companion object {
        private val INDONESIA_PHONE_REGEX = Regex("""^(\+62|62|0)8[1-9][0-9]{6,11}$""")
    }
}
