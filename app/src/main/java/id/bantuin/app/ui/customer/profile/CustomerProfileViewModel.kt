package id.bantuin.app.ui.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerProfileUiState(
    val user: UserProfile? = null,
    val activeRole: String = "customer",
    val isLoading: Boolean = false
)

class CustomerProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerProfileUiState())
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            authRepository.activeRole().collect { role ->
                _uiState.update { it.copy(activeRole = role) }
            }
        }
    }

    fun switchRole(newRole: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.setActiveRole(newRole)
            onComplete()
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
