package id.bantuin.app.usecase

import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.ConflictReason
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.usecase.scheduling.SchedulingConflictDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingConflictDetectorTest {

    private val detector = SchedulingConflictDetector()

    // Mitra mode ON 08:00–17:00 (480..1020); existing: blok 09:00–11:30 (540..690)
    private val dayOn = DayAvailability(dayOfWeek = 1, mode = AvailabilityMode.ON, openMin = 480, closeMin = 1020)
    private val existingList = listOf(TimeBlock(startMin = 540, endMin = 690))

    @Test
    fun testVector8_3_Scheduling() {
        // 1. Overlap: mulai 10:00 (600), durasi 60 -> blok 600..690 -> OVERLAP, alternatif: 11:30 (690), 12:00 (720), 12:30 (750)
        val r1 = detector.check(600, 60, dayOn, existingList, null)
        assertFalse(r1.valid)
        assertEquals(ConflictReason.OVERLAP, r1.reason)
        assertEquals(listOf(690, 720, 750), r1.alternativeStartsMin)

        // 2. Bersebelahan: mulai 11:30 (690), durasi 60 -> valid (end 690 == start 690)
        val r2 = detector.check(690, 60, dayOn, existingList, null)
        assertTrue(r2.valid)

        // 3. Di luar jam: mulai 16:00 (960), durasi 60 (blok sampai 17:30 = 1050 > 1020) -> OUTSIDE_HOURS, alternatif kosong
        val r3 = detector.check(960, 60, dayOn, existingList, null)
        assertFalse(r3.valid)
        assertEquals(ConflictReason.OUTSIDE_HOURS, r3.reason)
        assertTrue(r3.alternativeStartsMin.isEmpty())

        // 4. ALWAYS_ON batas: mulai 23:00 (1380), durasi 30 (blok sampai 1440 = 24:00) -> valid
        val dayAlways = DayAvailability(dayOfWeek = 1, mode = AvailabilityMode.ALWAYS_ON)
        val r4 = detector.check(1380, 30, dayAlways, emptyList(), null)
        assertTrue(r4.valid)

        // 5. ALWAYS_ON lewat: mulai 23:30 (1410), durasi 30 (blok sampai 1470 > 1440) -> OUTSIDE_HOURS
        val r5 = detector.check(1410, 30, dayAlways, emptyList(), null)
        assertFalse(r5.valid)
        assertEquals(ConflictReason.OUTSIDE_HOURS, r5.reason)

        // 6. Hari OFF -> DAY_OFF, alternatif kosong
        val dayOff = DayAvailability(dayOfWeek = 1, mode = AvailabilityMode.OFF)
        val r6 = detector.check(600, 60, dayOff, emptyList(), null)
        assertFalse(r6.valid)
        assertEquals(ConflictReason.DAY_OFF, r6.reason)
        assertTrue(r6.alternativeStartsMin.isEmpty())

        // 7. Masa lalu: hari ini, nowMin = 600 (10:00), mulai 09:00 (540) -> PAST_TIME
        val r7 = detector.check(540, 60, dayOn, emptyList(), 600)
        assertFalse(r7.valid)
        assertEquals(ConflictReason.PAST_TIME, r7.reason)
    }
}
