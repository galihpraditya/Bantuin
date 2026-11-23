package id.bantuin.app.ui.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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

    /**
     * Beralih peran hanya diperbolehkan bila akun memang memiliki role tersebut.
     * Untuk mode mitra, akun tanpa role "provider" diarahkan ke halaman login
     * (sesi dibersihkan) agar tidak menampilkan profil mitra demo orang lain.
     */
    fun switchRole(
        newRole: String,
        onAllowed: () -> Unit,
        onUnauthorized: () -> Unit
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser().firstOrNull()
            if (newRole == "provider" && user?.roles?.contains("provider") != true) {
                authRepository.signOut()
                onUnauthorized()
                return@launch
            }
            authRepository.setActiveRole(newRole)
            onAllowed()
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
