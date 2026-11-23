package id.bantuin.app.domain.usecase.trust

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.repository.MetricsRepository

class RecalculateProviderMetricsUseCase(private val repository: MetricsRepository) {
    suspend operator fun invoke(providerId: String): AppResult<Unit> =
        repository.refreshTrust(providerId)
}
