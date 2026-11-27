package id.bantuin.app.domain.usecase.matchmaking

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.MatchResult
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RadiusExpansionState
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
    operator fun invoke(
        customer: GeoPoint,
        candidates: List<ProviderCandidate>,
        radiusKm: Double = BantuinConfig.MAX_RADIUS_KM
    ): MatchResult {
        return invokeWithDistances(
            candidates.map { it to HaversineCalculator.distanceKm(customer, it.location) },
            radiusKm = radiusKm
        )
    }

    /**
     * Memungkinkan pemanggilan langsung dengan jarak yang sudah dihitung (berguna untuk test vector).
     */
    fun invokeWithDistances(
        candidatesWithDistance: List<Pair<ProviderCandidate, Double>>,
        radiusKm: Double = BantuinConfig.MAX_RADIUS_KM
    ): MatchResult {
        val radius = radiusKm.coerceAtLeast(0.1)
        val (inRange, out) = candidatesWithDistance.partition { it.second <= radius }
        val ranked = inRange.map { (c, d) ->
            val ds = (1.0 - d / radius).coerceIn(0.0, 1.0)
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

/**
 * Hasil matchmaking dengan dukungan ekspansi radius dinamis.
 */
data class ExpandedMatchResult(
    val matchResult: MatchResult,
    val effectiveRadiusKm: Double,
    val expansionState: RadiusExpansionState,
    val potentialCountIfExpanded: Int
)

/**
 * SCALE FEATURE: Dynamic Radius Expansion (F1 Extension)
 *
 * Jika jumlah mitra dalam radius default kurang dari [BantuinConfig.MIN_PROVIDERS_BEFORE_EXPAND],
 * periksa apakah ada mitra dalam radius diperluas ([BantuinConfig.MAX_RADIUS_KM_EXPANDED]).
 * Jika pengguna sudah menyetujui ekspansi (`allowExpanded = true`), jalankan matchmaking pada radius diperluas.
 */
class DynamicRadiusExpansionUseCase(
    private val matchmakingUseCase: MatchmakingUseCase = MatchmakingUseCase()
) {
    operator fun invoke(
        customer: GeoPoint,
        candidates: List<ProviderCandidate>,
        defaultRadiusKm: Double = BantuinConfig.MAX_RADIUS_KM_DEFAULT,
        expandedRadiusKm: Double = BantuinConfig.MAX_RADIUS_KM_EXPANDED,
        allowExpanded: Boolean = false
    ): ExpandedMatchResult {
        if (allowExpanded) {
            val expanded = matchmakingUseCase(customer, candidates, radiusKm = expandedRadiusKm)
            return ExpandedMatchResult(
                matchResult = expanded,
                effectiveRadiusKm = expandedRadiusKm,
                expansionState = RadiusExpansionState.EXPANDED,
                potentialCountIfExpanded = 0
            )
        }

        val normal = matchmakingUseCase(customer, candidates, radiusKm = defaultRadiusKm)
        if (normal.ranked.size >= BantuinConfig.MIN_PROVIDERS_BEFORE_EXPAND) {
            return ExpandedMatchResult(
                matchResult = normal,
                effectiveRadiusKm = defaultRadiusKm,
                expansionState = RadiusExpansionState.NORMAL,
                potentialCountIfExpanded = 0
            )
        }

        val expandedPreview = matchmakingUseCase(customer, candidates, radiusKm = expandedRadiusKm)
        val state = if (expandedPreview.ranked.size > normal.ranked.size) {
            RadiusExpansionState.SUGGESTING_EXPANSION
        } else {
            RadiusExpansionState.NORMAL
        }
        return ExpandedMatchResult(
            matchResult = normal,
            effectiveRadiusKm = defaultRadiusKm,
            expansionState = state,
            potentialCountIfExpanded = expandedPreview.ranked.size
        )
    }
}
