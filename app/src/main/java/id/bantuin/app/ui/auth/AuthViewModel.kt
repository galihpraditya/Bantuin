package id.bantuin.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val phone: String = "",
    val isProvider: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = null) }
    }

    fun onRoleToggle(isProvider: Boolean) {
        _uiState.update { it.copy(isProvider = isProvider) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signIn(onSuccess: (UserProfile?) -> Unit) {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email dan password wajib diisi") }
            return
        }
        if (!EMAIL_REGEX.matches(email)) {
            _uiState.update { it.copy(errorMessage = "Format email tidak valid") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signIn(email, password)) {
                is AppResult.Success -> {
                    val user = authRepository.currentUser().firstOrNull()
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess(user)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is AppResult.Loading -> {
                    // Sedang diproses
                }
            }
        }
    }

    fun signUp(onSuccess: (UserProfile?) -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        val email = state.email.trim()
        val password = state.password
        val normalizedPhone = normalizePhone(state.phone)

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama, email, dan password wajib diisi") }
            return
        }
        if (!EMAIL_REGEX.matches(email)) {
            _uiState.update { it.copy(errorMessage = "Format email tidak valid") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Kata sandi minimal 8 karakter") }
            return
        }
        if (normalizedPhone.isNotBlank() && !INDONESIA_PHONE_REGEX.matches(normalizedPhone)) {
            _uiState.update {
                it.copy(errorMessage = "Nomor telepon Indonesia tidak valid (contoh: 081234567890)")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signUp(
                name = name,
                email = email,
                pass = password,
                phone = normalizedPhone,
                isProvider = state.isProvider
            )
            when (result) {
                is AppResult.Success -> {
                    val user = authRepository.currentUser().firstOrNull()
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess(user)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is AppResult.Loading -> {
                    // Sedang diproses
                }
            }
        }
    }

    private fun normalizePhone(raw: String): String {
        return raw.trim().replace(" ", "").replace("-", "")
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val INDONESIA_PHONE_REGEX = Regex("^(\\+62|62|0)8[1-9][0-9]{7,11}$")
    }
}
