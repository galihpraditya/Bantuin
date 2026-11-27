package id.bantuin.app.domain.usecase.trust

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.MetricsRepository

class RecalculateProviderMetricsUseCase(
    private val metricsRepository: MetricsRepository,
    private val trustScoreUseCase: TrustScoreUseCase = TrustScoreUseCase()
) {
    suspend operator fun invoke(providerId: String): AppResult<TrustResult> {
        return when (val inputResult = metricsRepository.trustInput(providerId, 30)) {
            is AppResult.Success -> {
                val evaluated = trustScoreUseCase.calculate(inputResult.data)
                when (val result = metricsRepository.apply(providerId, evaluated)) {
                    is AppResult.Success -> AppResult.Success(evaluated)
                    is AppResult.Error -> result
                    AppResult.Loading -> AppResult.Loading
                }
            }
            is AppResult.Error -> AppResult.Error(inputResult.message, inputResult.cause)
            AppResult.Loading -> AppResult.Loading
        }
    }
}
