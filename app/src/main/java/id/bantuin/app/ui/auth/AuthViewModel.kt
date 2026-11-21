package id.bantuin.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun signIn(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email dan password wajib diisi") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signIn(state.email, state.password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess(result.data)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun signUp(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama, email, dan password wajib diisi") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signUp(
                name = state.name,
                email = state.email,
                pass = state.password,
                phone = state.phone,
                isProvider = state.isProvider
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess(result.data)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}
