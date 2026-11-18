package id.bantuin.app.domain.usecase.matchmaking

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.MatchResult
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RankedProvider
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineCalculator {
    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) *
                sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(h)))
    }
}

/**
 * F1 - Smart Service Matchmaking
 * Mengurutkan mitra berdasarkan Haversine distance, rating kepuasan, dan rasio penyelesaian.
 * Memberikan penalti 0.7x jika mitra terdeteksi anomali.
 */
class MatchmakingUseCase {
    operator fun invoke(customer: GeoPoint, candidates: List<ProviderCandidate>): MatchResult {
        return invokeWithDistances(candidates.map { it to HaversineCalculator.distanceKm(customer, it.location) })
    }

    /**
     * Memungkinkan pemanggilan langsung dengan jarak yang sudah dihitung (berguna untuk test vector).
     */
    fun invokeWithDistances(candidatesWithDistance: List<Pair<ProviderCandidate, Double>>): MatchResult {
        val (inRange, out) = candidatesWithDistance.partition { it.second <= BantuinConfig.MAX_RADIUS_KM }
        val ranked = inRange.map { (c, d) ->
            val ds = 1.0 - d / BantuinConfig.MAX_RADIUS_KM
            val rs = c.ratingAvg?.div(5.0) ?: BantuinConfig.DEFAULT_RATING_SCORE
            val cs = c.completionRate ?: BantuinConfig.DEFAULT_COMPLETION_SCORE
            var s = BantuinConfig.W_DISTANCE * ds +
                    BantuinConfig.W_RATING * rs +
                    BantuinConfig.W_COMPLETION * cs
            if (c.flagAnomaly) s *= BantuinConfig.ANOMALY_PENALTY_FACTOR
            RankedProvider(
                candidate = c,
                distanceKm = d,
                distanceScore = ds,
                ratingScore = rs,
                completionScore = cs,
                finalScore = s,
                matchScore = (s * 100).roundToInt()
            )
        }.sortedWith(
            compareByDescending<RankedProvider> { it.finalScore }
                .thenByDescending { it.candidate.ratingAvg ?: 0.0 }
                .thenBy { it.distanceKm }
        )
        return MatchResult(ranked, out.map { it.first })
    }
}
