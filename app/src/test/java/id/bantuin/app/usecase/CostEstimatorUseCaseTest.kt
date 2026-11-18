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
}
