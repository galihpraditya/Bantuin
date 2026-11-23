package id.bantuin.app.usecase

import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.usecase.matchmaking.HaversineCalculator
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MatchmakingUseCaseTest {

    private val useCase = MatchmakingUseCase()

    @Test
    fun testHaversineDistance_samePointAndDegreeDelta() {
        val p1 = GeoPoint(-7.9797, 112.6304)
        val distZero = HaversineCalculator.distanceKm(p1, p1)
        assertEquals(0.0, distZero, 0.0001)

        val p2 = GeoPoint(-7.9697, 112.6304) // selisih 0.01 lintang
        val distDelta = HaversineCalculator.distanceKm(p1, p2)
        assertTrue("Jarak harus ~1.11 km, aktual: $distDelta", abs(distDelta - 1.11) <= 0.02)
    }

    @Test
    fun testVector8_1_Matchmaking() {
        val candidateA = ProviderCandidate(
            providerId = "A", name = "Mitra A", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 4.5, completionRate = 0.9,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )
        val candidateB = ProviderCandidate(
            providerId = "B", name = "Mitra B", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 4.0, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )
        val candidateC = ProviderCandidate(
            providerId = "C", name = "Mitra C", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 5.0, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )
        val candidateD = ProviderCandidate(
            providerId = "D", name = "Mitra D", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 5.0, completionRate = 1.0,
            flagAnomaly = true, badge = ProviderBadge.NONE, minBaseFare = 50000
        )
        val candidateE = ProviderCandidate(
            providerId = "E", name = "Mitra E", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = null, completionRate = null,
            flagAnomaly = false, badge = ProviderBadge.NEW, minBaseFare = 50000
        )

        val input = listOf(
            candidateA to 2.0,
            candidateB to 1.0,
            candidateC to 11.0,
            candidateD to 0.5,
            candidateE to 10.0
        )

        val result = useCase.invokeWithDistances(input)

        // C di luar radius 10 km
        assertEquals(1, result.outOfRange.size)
        assertEquals("C", result.outOfRange[0].providerId)

        // Urutan: B (88), A (85), D (68), E (35)
        assertEquals(4, result.ranked.size)

        val b = result.ranked[0]
        assertEquals("B", b.candidate.providerId)
        assertEquals(88, b.matchScore)

        val a = result.ranked[1]
        assertEquals("A", a.candidate.providerId)
        assertEquals(85, a.matchScore)

        val d = result.ranked[2]
        assertEquals("D", d.candidate.providerId)
        assertEquals(68, d.matchScore)

        val e = result.ranked[3]
        assertEquals("E", e.candidate.providerId)
        assertEquals(35, e.matchScore)
    }

    @Test
    fun testEmptyCandidates_returnsEmptyResults() {
        val customer = GeoPoint(-7.9797, 112.6304)
        val result = useCase.invoke(customer, emptyList())
        assertTrue(result.ranked.isEmpty())
        assertTrue(result.outOfRange.isEmpty())
    }

    @Test
    fun testAllOutOfRange_returnsAllInOutList() {
        val candidateFar1 = ProviderCandidate(
            providerId = "far1", name = "Mitra Far 1", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 5.0, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )
        val candidateFar2 = ProviderCandidate(
            providerId = "far2", name = "Mitra Far 2", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 4.8, completionRate = 0.95,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )

        val input = listOf(
            candidateFar1 to 10.05,
            candidateFar2 to 15.0
        )
        val result = useCase.invokeWithDistances(input)

        assertTrue(result.ranked.isEmpty())
        assertEquals(2, result.outOfRange.size)
        assertEquals("far1", result.outOfRange[0].providerId)
        assertEquals("far2", result.outOfRange[1].providerId)
    }

    @Test
    fun testBoundaryDistance10Km_inclusiveThreshold() {
        val boundaryCandidate = ProviderCandidate(
            providerId = "bound", name = "Mitra Boundary", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 5.0, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )
        val outsideCandidate = ProviderCandidate(
            providerId = "out", name = "Mitra Outside", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 5.0, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )

        val result = useCase.invokeWithDistances(listOf(
            boundaryCandidate to 10.0,
            outsideCandidate to 10.0001
        ))

        assertEquals(1, result.ranked.size)
        assertEquals("bound", result.ranked[0].candidate.providerId)
        assertEquals(0.0, result.ranked[0].distanceScore, 0.0001)

        assertEquals(1, result.outOfRange.size)
        assertEquals("out", result.outOfRange[0].providerId)
    }

    @Test
    fun testTieBreakers_equalFinalScore_tieBreaksByHigherRating_thenShorterDistance() {
        // Δd = 1.4 * Δrating
        // candHighRating: rating 4.5, distance 2.7 km -> 0.5*0.73 (0.365) + 0.35*0.9 (0.315) = 0.680
        // candLowRating:  rating 4.0, distance 2.0 km -> 0.5*0.80 (0.400) + 0.35*0.8 (0.280) = 0.680
        val candHighRating = ProviderCandidate(
            providerId = "cand_high_rating", name = "Mitra High Rating", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 4.5, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )
        val candLowRating = ProviderCandidate(
            providerId = "cand_low_rating", name = "Mitra Low Rating", avatarUrl = null,
            location = GeoPoint(0.0, 0.0), ratingAvg = 4.0, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )

        val result = useCase.invokeWithDistances(listOf(
            candLowRating to 2.0,
            candHighRating to 2.7
        ))

        assertEquals(2, result.ranked.size)
        assertEquals(result.ranked[0].finalScore, result.ranked[1].finalScore, 0.00000001)

        // Jika finalScore identik, candHighRating harus di urutan pertama karena rating 4.5 > 4.0
        assertEquals("cand_high_rating", result.ranked[0].candidate.providerId)
        assertEquals("cand_low_rating", result.ranked[1].candidate.providerId)
    }

    @Test
    fun testInvokeWithGeoPoints_calculatesDistanceCorrectly() {
        val customer = GeoPoint(-7.9797, 112.6304) // Alun-alun Malang
        val candidateNear = ProviderCandidate(
            providerId = "near", name = "Mitra Dekat", avatarUrl = null,
            location = GeoPoint(-7.9697, 112.6304), // ~1.11 km
            ratingAvg = 4.5, completionRate = 0.95,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )
        val candidateFar = ProviderCandidate(
            providerId = "far", name = "Mitra Jauh", avatarUrl = null,
            location = GeoPoint(-7.8000, 112.6304), // > 19 km
            ratingAvg = 5.0, completionRate = 1.0,
            flagAnomaly = false, badge = ProviderBadge.TRUSTED, minBaseFare = 50000
        )

        val result = useCase.invoke(customer, listOf(candidateNear, candidateFar))

        assertEquals(1, result.ranked.size)
        assertEquals("near", result.ranked[0].candidate.providerId)
        assertTrue(result.ranked[0].distanceKm in 1.0..1.2)

        assertEquals(1, result.outOfRange.size)
        assertEquals("far", result.outOfRange[0].providerId)
    }
}
