package id.bantuin.app.domain.usecase.pricing

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.CostEstimate
import id.bantuin.app.domain.model.SurgeReason
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * F2 - Dynamic Cost & Surge Estimator
 * Menghitung estimasi tarif transparan:
 * raw = (baseFare + jarakKm * 2000) * bobotKompleksitas (jarak dibulatkan 1 desimal HALF_UP)
 * baseCost = dibulatkan ke atas kelipatan 500
 * surge = +20% tunggal jika jam sibuk, malam, atau darurat.
 */
class CostEstimatorUseCase {
    operator fun invoke(
        baseFare: Int,
        distanceKm: Double,
        complexity: Complexity,
        startMinOfDay: Int,
        isUrgent: Boolean
    ): CostEstimate {
        val dist = BigDecimal.valueOf(distanceKm).setScale(1, RoundingMode.HALF_UP)
        val distCost = dist.multiply(BigDecimal(BantuinConfig.RATE_PER_KM))
        val raw = (BigDecimal(baseFare) + distCost).multiply(complexity.weight)
        val baseCost = roundUp(raw)

        val peak = BantuinConfig.PEAK_RANGES.any { startMinOfDay in it }
        val night = startMinOfDay >= BantuinConfig.NIGHT_START_MIN || startMinOfDay < BantuinConfig.NIGHT_END_MIN
        val surge = peak || night || isUrgent

        val total = if (surge) {
            roundUp(raw.multiply(BigDecimal(BantuinConfig.SURGE_MULTIPLIER)))
        } else {
            baseCost
        }

        val reason = when {
            !surge -> SurgeReason.NONE
            peak && isUrgent -> SurgeReason.PEAK_AND_URGENT
            night && isUrgent -> SurgeReason.NIGHT_AND_URGENT
            peak -> SurgeReason.PEAK_HOUR
            night -> SurgeReason.NIGHT
            else -> SurgeReason.URGENT
        }

        return CostEstimate(
            baseFare = baseFare,
            distanceKm = dist.toDouble(),
            distanceCost = distCost.setScale(0, RoundingMode.HALF_UP).toInt(),
            complexity = complexity,
            baseCost = baseCost,
            surgeApplied = surge,
            surgeAmount = total - baseCost,
            surgeReason = reason,
            total = total
        )
    }

    private fun roundUp(x: BigDecimal): Int =
        x.divide(BigDecimal(BantuinConfig.ROUNDING_STEP), 0, RoundingMode.CEILING)
            .multiply(BigDecimal(BantuinConfig.ROUNDING_STEP)).toInt()
}
