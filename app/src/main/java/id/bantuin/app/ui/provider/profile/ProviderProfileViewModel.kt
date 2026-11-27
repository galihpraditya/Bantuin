package id.bantuin.app.ui.provider.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderProfileUiState(
    val user: UserProfile? = null,
    val provider: ProviderCandidate? = null,
    val trustResult: TrustResult? = null,
    val isAcceptingOrders: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ProviderProfileViewModel(
    private val authRepository: AuthRepository,
    private val catalogRepository: CatalogRepository,
    private val metricsRepository: MetricsRepository,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderProfileUiState())
    val uiState: StateFlow<ProviderProfileUiState> = _uiState.asStateFlow()

    private var acceptingJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
                if (user != null) {
                    loadProviderDetails(user.id)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun refresh() {
        val uid = _uiState.value.user?.id ?: return
        loadProviderDetails(uid)
    }

    private fun loadProviderDetails(providerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = catalogRepository.providerProfile(providerId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(provider = res.data, isLoading = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is AppResult.Loading -> {}
            }

            when (val trustRes = metricsRepository.trustInput(providerId, BantuinConfig.TRUST_WINDOW_DAYS)) {
                is AppResult.Success -> {
                    val trust = trustScoreUseCase.calculate(trustRes.data)
                    _uiState.update { it.copy(trustResult = trust) }
                }
                else -> {}
            }
        }

        acceptingJob?.cancel()
        acceptingJob = viewModelScope.launch {
            catalogRepository.observeAcceptingOrders(providerId).collect { accepting ->
                _uiState.update { it.copy(isAcceptingOrders = accepting) }
            }
        }
    }
}
