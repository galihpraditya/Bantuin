package id.bantuin.app.domain.usecase.trust

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.AnomalyReason
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration.Companion.minutes

/**
 * F4 - Provider Trust Score & Anomaly Analyzer
 * Menghitung Trust Score mitra dalam rentang 0..100 berbasis jendela 30 hari.
 * Rumus: 0.45 * Skor Rating + 0.35 * On-Time SLA + 0.20 * (100 - Cancellation Rate)
 * Evaluasi anomali aktif bila tx >= 5 dan (Trust Score < 60 atau Cancel Rate > 20%).
 */
class TrustScoreUseCase {
    operator fun invoke(input: TrustInput): TrustResult = calculate(input)

    fun calculate(input: TrustInput): TrustResult {
        val done = input.completed.size
        val tx = done + input.mitraCancelledCount
        val ratingAvg = input.ratings.takeIf { it.isNotEmpty() }?.average()
        val ratingScore = ratingAvg?.let { it / 5.0 * 100.0 } ?: 70.0
        val tolerance = BantuinConfig.LATE_TOLERANCE_MIN.minutes

        val onTimeRate = if (done == 0) {
            0.0
        } else {
            input.completed.count { it.startedAt <= it.startAt + tolerance } * 100.0 / done
        }

        val cancelRate = if (tx == 0) 0.0 else input.mitraCancelledCount * 100.0 / tx
        val completion = if (tx == 0) null else 1.0 - cancelRate / 100.0

        val computed = BantuinConfig.W_TRUST_RATING * ratingScore +
                BantuinConfig.W_TRUST_ONTIME * onTimeRate +
                BantuinConfig.W_TRUST_RELIABILITY * (100.0 - cancelRate)

        if (tx < BantuinConfig.MIN_TX_FOR_EVALUATION) {
            return TrustResult(
                ratingAvg = ratingAvg,
                ratingScore = ratingScore,
                onTimeRate = onTimeRate,
                cancelRate = cancelRate,
                completionRate = completion,
                trustScore = BantuinConfig.NEW_PROVIDER_TRUST,
                txCount = tx,
                flagAnomaly = false,
                badge = ProviderBadge.NEW,
                reasons = emptyList()
            )
        }

        val score = BigDecimal(computed).setScale(2, RoundingMode.HALF_UP).toDouble()
        val reasons = buildList {
            if (score < BantuinConfig.TRUST_MIN_THRESHOLD) add(AnomalyReason.LOW_TRUST_SCORE)
            if (cancelRate > BantuinConfig.CANCEL_RATE_MAX_PCT) add(AnomalyReason.HIGH_CANCEL_RATE)
        }
        val flag = reasons.isNotEmpty()

        return TrustResult(
            ratingAvg = ratingAvg,
            ratingScore = ratingScore,
            onTimeRate = onTimeRate,
            cancelRate = cancelRate,
            completionRate = completion,
            trustScore = score,
            txCount = tx,
            flagAnomaly = flag,
            badge = if (flag) ProviderBadge.NONE else ProviderBadge.TRUSTED,
            reasons = reasons
        )
    }
}
