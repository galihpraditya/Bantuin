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
}
