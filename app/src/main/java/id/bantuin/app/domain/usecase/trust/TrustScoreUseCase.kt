package id.bantuin.app.domain.usecase.trust

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.AnomalyReason
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.model.CompletionOutcome
import kotlin.time.Duration.Companion.minutes

class TrustScoreUseCase {
    operator fun invoke(input: TrustInput): TrustResult = calculate(input)

    fun calculate(input: TrustInput): TrustResult {
        val done = input.completed.size
        val cancelled = input.mitraCancelledCount
        val total = done + cancelled + input.otherOrderCount
        if (total == 0) {
            // Mitra benar-benar baru belum punya riwayat; gunakan skor awal agar
            // tidak dianggap anomali dan tidak kena penalti visibilitas 0.7x.
            return TrustResult(
                ratingAvg = null,
                ratingScore = 0.0,
                onTimeRate = 0.0,
                cancelRate = 0.0,
                completionRate = null,
                trustScore = BantuinConfig.TRUST_NEW_PROVIDER_SCORE,
                txCount = 0,
                flagAnomaly = false,
                badge = ProviderBadge.NEW,
                reasons = emptyList()
            )
        }
        val onTime = input.completed.count { job ->
            job.outcome?.let { it == CompletionOutcome.SELESAI_TEPAT_WAKTU }
                ?: (job.startedAt <= job.startAt + BantuinConfig.LATE_TOLERANCE_MIN.minutes)
        }
        val average = input.ratings.takeIf { it.isNotEmpty() }?.average()
        val ratingScore = (average ?: 0.0) * 20
        val onTimeRate = if (done == 0) 0.0 else 100.0 * onTime / done
        val cancelRate = if (total == 0) 0.0 else 100.0 * cancelled / total

        // Compare the exact weighted fractions before dividing so 60 and 20 remain strict thresholds.
        val ratingsDen = input.ratings.size.coerceAtLeast(1).toBigDecimal()
        val slaDen = done.coerceAtLeast(1).toBigDecimal()
        val cancelDen = total.coerceAtLeast(1).toBigDecimal()
        val denominator = ratingsDen * slaDen * cancelDen
        val numerator = (BantuinConfig.W_TRUST_RATING.toBigDecimal() * 20.toBigDecimal()) * input.ratings.sumOf { it.toLong() }.toBigDecimal() * slaDen * cancelDen +
            (BantuinConfig.W_TRUST_ONTIME.toBigDecimal() * 100.toBigDecimal()) * onTime.toBigDecimal() * ratingsDen * cancelDen -
            (BantuinConfig.W_TRUST_CANCELLATION.toBigDecimal() * 100.toBigDecimal()) * cancelled.toBigDecimal() * ratingsDen * slaDen
        val score = numerator.toDouble().div(denominator.toDouble()).coerceIn(0.0, 100.0)
        val reasons = buildList {
            if (numerator < 60.toBigDecimal() * denominator) add(AnomalyReason.LOW_TRUST_SCORE)
            if (5L * cancelled > total) add(AnomalyReason.HIGH_CANCEL_RATE)
        }
        return TrustResult(
            ratingAvg = average, ratingScore = ratingScore, onTimeRate = onTimeRate, cancelRate = cancelRate,
            completionRate = (done + cancelled).takeIf { it > 0 }?.let { done.toDouble() / it },
            trustScore = score, txCount = total, flagAnomaly = reasons.isNotEmpty(),
            badge = if (reasons.isEmpty()) ProviderBadge.TRUSTED else ProviderBadge.NONE, reasons = reasons
        )
    }
}
