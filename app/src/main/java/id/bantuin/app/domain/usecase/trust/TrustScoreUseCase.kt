package id.bantuin.app.domain.usecase.trust

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.AnomalyReason
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult

class TrustScoreUseCase {
    operator fun invoke(input: TrustInput): TrustResult = calculate(input)

    fun calculate(input: TrustInput): TrustResult {
        val completed = input.onTimeCount + input.lateCount
        val total = input.transactionCount
        val ratingAvg = input.ratingCount.takeIf { it > 0 }?.let { input.ratingTotal.toDouble() / it }
        val ratingScore = (ratingAvg ?: 0.0) / 5.0 * 100.0
        val onTimeRate = percentage(input.onTimeCount, completed)
        val cancelRate = percentage(input.cancelledCount, total)
        // Compare the weighted fractions before division so exact thresholds stay exact.
        val ratingDenominator = input.ratingCount.coerceAtLeast(1).toBigDecimal()
        val slaDenominator = completed.coerceAtLeast(1).toBigDecimal()
        val cancelDenominator = total.coerceAtLeast(1).toBigDecimal()
        val denominator = ratingDenominator * slaDenominator * cancelDenominator
        val ratingPoints = input.ratingTotal.toBigDecimal() * RATING_WEIGHT * slaDenominator * cancelDenominator
        val slaPoints = input.onTimeCount.toBigDecimal() * SLA_WEIGHT * ratingDenominator * cancelDenominator
        val cancellationPoints = input.cancelledCount.toBigDecimal() * CANCELLATION_WEIGHT * ratingDenominator * slaDenominator
        val numerator = ratingPoints + slaPoints - cancellationPoints
        val score = (numerator.toDouble() / denominator.toDouble()).coerceIn(0.0, 100.0)
        val reasons = buildList {
            if (numerator < BantuinConfig.TRUST_MIN_THRESHOLD.toBigDecimal() * denominator) {
                add(AnomalyReason.LOW_TRUST_SCORE)
            }
            if (input.cancelledCount * 100L > total * BantuinConfig.CANCEL_RATE_MAX_PCT.toLong()) {
                add(AnomalyReason.HIGH_CANCEL_RATE)
            }
        }
        return TrustResult(
            ratingAvg = ratingAvg,
            ratingScore = ratingScore,
            onTimeRate = onTimeRate,
            cancelRate = cancelRate,
            completionRate = total.takeIf { it > 0 }?.let { completed.toDouble() / it },
            trustScore = score,
            txCount = total,
            flagAnomaly = reasons.isNotEmpty(),
            badge = if (reasons.isEmpty()) ProviderBadge.TRUSTED else ProviderBadge.NONE,
            reasons = reasons
        )
    }

    private companion object {
        val RATING_WEIGHT = BantuinConfig.W_TRUST_RATING.toBigDecimal() * 20.toBigDecimal()
        val SLA_WEIGHT = BantuinConfig.W_TRUST_ONTIME.toBigDecimal() * 100.toBigDecimal()
        val CANCELLATION_WEIGHT = BantuinConfig.W_TRUST_CANCELLATION.toBigDecimal() * 100.toBigDecimal()
    }

    private fun percentage(count: Int, total: Int): Double =
        if (total == 0) 0.0 else count * 100.0 / total
}
