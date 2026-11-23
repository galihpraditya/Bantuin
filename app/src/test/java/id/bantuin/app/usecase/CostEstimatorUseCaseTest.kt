package id.bantuin.app.usecase

import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.SurgeReason
import id.bantuin.app.domain.usecase.pricing.CostEstimatorUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CostEstimatorUseCaseTest {

    private val useCase = CostEstimatorUseCase()

    @Test
    fun testVector8_2_CostEstimator() {
        // 1. baseFare 100000, 3.0 km, SEDANG, 10:00 (600 min), urgent false -> total 132500, surge false
        val r1 = useCase(100000, 3.0, Complexity.SEDANG, 600, false)
        assertEquals(132500, r1.baseCost)
        assertEquals(132500, r1.total)
        assertFalse(r1.surgeApplied)

        // 2. 100000, 3.0, SEDANG, 17:00 (1020 min), urgent false -> total 159000, surge PEAK_HOUR
        val r2 = useCase(100000, 3.0, Complexity.SEDANG, 1020, false)
        assertEquals(132500, r2.baseCost)
        assertEquals(159000, r2.total)
        assertTrue(r2.surgeApplied)
        assertEquals(26500, r2.surgeAmount)
        assertEquals(SurgeReason.PEAK_HOUR, r2.surgeReason)

        // 3. 50000, 2.5, RINGAN, 10:00 (600 min), urgent true -> total 66000, URGENT
        val r3 = useCase(50000, 2.5, Complexity.RINGAN, 600, true)
        assertEquals(55000, r3.baseCost)
        assertEquals(66000, r3.total)
        assertTrue(r3.surgeApplied)
        assertEquals(SurgeReason.URGENT, r3.surgeReason)

        // 4. 75000, 1.3, BERAT, 10:00 (600 min), urgent false -> total 116500
        val r4 = useCase(75000, 1.3, Complexity.BERAT, 600, false)
        assertEquals(116500, r4.baseCost)
        assertEquals(116500, r4.total)
        assertFalse(r4.surgeApplied)

        // 5. 75000, 1.3, BERAT, 21:00 (1260 min), urgent false -> total 140000, NIGHT
        val r5 = useCase(75000, 1.3, Complexity.BERAT, 1260, false)
        assertEquals(116500, r5.baseCost)
        assertEquals(140000, r5.total)
        assertTrue(r5.surgeApplied)
        assertEquals(23500, r5.surgeAmount)
        assertEquals(SurgeReason.NIGHT, r5.surgeReason)

        // 6. 100000, 3.0, SEDANG, 17:00 (1020 min), urgent true -> total 159000 (tidak menumpuk)
        val r6 = useCase(100000, 3.0, Complexity.SEDANG, 1020, true)
        assertEquals(132500, r6.baseCost)
        assertEquals(159000, r6.total)
        assertEquals(SurgeReason.PEAK_AND_URGENT, r6.surgeReason)

        // 7. 100000, 0.0, RINGAN, 06:00 (360 min), urgent false -> total 100000 (batas 06:00 = normal)
        val r7 = useCase(100000, 0.0, Complexity.RINGAN, 360, false)
        assertEquals(100000, r7.total)
        assertFalse(r7.surgeApplied)

        // 8. 100000, 0.0, RINGAN, 05:59 (359 min), urgent false -> total 120000 (NIGHT)
        val r8 = useCase(100000, 0.0, Complexity.RINGAN, 359, false)
        assertEquals(120000, r8.total)
        assertTrue(r8.surgeApplied)
        assertEquals(SurgeReason.NIGHT, r8.surgeReason)
    }

    @Test
    fun testZeroDistance_calculatesCorrectlyWithoutDistanceCost() {
        val result = useCase(
            baseFare = 80000,
            distanceKm = 0.0,
            complexity = Complexity.RINGAN,
            startMinOfDay = 600,
            isUrgent = false
        )
        assertEquals(0, result.distanceCost)
        assertEquals(80000, result.baseCost)
        assertEquals(80000, result.total)
        assertFalse(result.surgeApplied)
        assertEquals(SurgeReason.NONE, result.surgeReason)
    }

    @Test
    fun testRoundingUpCeilingTo500() {
        // baseFare = 45100, dist = 0.0, RINGAN (weight 1.0) -> raw = 45100 -> roundUp500 = 45500
        val r = useCase(
            baseFare = 45100,
            distanceKm = 0.0,
            complexity = Complexity.RINGAN,
            startMinOfDay = 600,
            isUrgent = false
        )
        assertEquals(45500, r.baseCost)
        assertEquals(45500, r.total)
    }

    @Test
    fun testPeakHourExactBoundaries() {
        // PEAK_RANGES in BantuinConfig: 07:00-08:59 (420..539), 16:00-18:59 (960..1139)
        // 420 (07:00) should be PEAK_HOUR
        val rMorningStart = useCase(50000, 1.0, Complexity.RINGAN, 420, false)
        assertTrue(rMorningStart.surgeApplied)
        assertEquals(SurgeReason.PEAK_HOUR, rMorningStart.surgeReason)

        // 539 (08:59) should be PEAK_HOUR
        val rMorningEnd = useCase(50000, 1.0, Complexity.RINGAN, 539, false)
        assertTrue(rMorningEnd.surgeApplied)
        assertEquals(SurgeReason.PEAK_HOUR, rMorningEnd.surgeReason)

        // 540 (09:00) should be normal (NONE)
        val rAfterMorning = useCase(50000, 1.0, Complexity.RINGAN, 540, false)
        assertFalse(rAfterMorning.surgeApplied)
        assertEquals(SurgeReason.NONE, rAfterMorning.surgeReason)

        // 960 (16:00) should be PEAK_HOUR
        val rEveningStart = useCase(50000, 1.0, Complexity.RINGAN, 960, false)
        assertTrue(rEveningStart.surgeApplied)
        assertEquals(SurgeReason.PEAK_HOUR, rEveningStart.surgeReason)

        // 1139 (18:59) should be PEAK_HOUR
        val rEveningEnd = useCase(50000, 1.0, Complexity.RINGAN, 1139, false)
        assertTrue(rEveningEnd.surgeApplied)
        assertEquals(SurgeReason.PEAK_HOUR, rEveningEnd.surgeReason)

        // 1140 (19:00) should be normal (NONE)
        val rAfterEvening = useCase(50000, 1.0, Complexity.RINGAN, 1140, false)
        assertFalse(rAfterEvening.surgeApplied)
        assertEquals(SurgeReason.NONE, rAfterEvening.surgeReason)
    }

    @Test
    fun testNightBoundaryAndNightPlusUrgent() {
        // NIGHT_START_MIN = 20:00 (1200), NIGHT_END_MIN = 06:00 (360)
        // 1199 (19:59) is normal
        val rBeforeNight = useCase(50000, 1.0, Complexity.RINGAN, 1199, false)
        assertFalse(rBeforeNight.surgeApplied)
        assertEquals(SurgeReason.NONE, rBeforeNight.surgeReason)

        // 1200 (20:00) is NIGHT
        val rNightStart = useCase(50000, 1.0, Complexity.RINGAN, 1200, false)
        assertTrue(rNightStart.surgeApplied)
        assertEquals(SurgeReason.NIGHT, rNightStart.surgeReason)

        // 1200 (20:00) with urgent = true -> NIGHT_AND_URGENT (surge 1.20x single)
        val rNightAndUrgent = useCase(50000, 1.0, Complexity.RINGAN, 1200, true)
        assertTrue(rNightAndUrgent.surgeApplied)
        assertEquals(SurgeReason.NIGHT_AND_URGENT, rNightAndUrgent.surgeReason)
        assertEquals(rNightStart.total, rNightAndUrgent.total)
    }
}
