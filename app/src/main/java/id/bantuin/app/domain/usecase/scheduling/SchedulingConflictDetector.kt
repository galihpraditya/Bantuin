package id.bantuin.app.domain.usecase.scheduling

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.ConflictReason
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.SlotCheckResult
import id.bantuin.app.domain.model.TimeBlock

/**
 * F3 - Automated SLA & Scheduling Conflict Detector
 * Memvalidasi slot jadwal pelanggan terhadap jam operasional mitra dan pesanan existing.
 * Jika terjadi konflik, menyarankan hingga 3 slot alternatif bebas tumpang tindih.
 */
class SchedulingConflictDetector {
    fun blockFor(startMin: Int, durationMin: Int) =
        TimeBlock(startMin, startMin + durationMin + BantuinConfig.TRAVEL_BUFFER_MIN)

    fun overlaps(a: TimeBlock, b: TimeBlock) = a.startMin < b.endMin && b.startMin < a.endMin

    private fun window(day: DayAvailability): TimeBlock? = when (day.mode) {
        AvailabilityMode.OFF -> null
        AvailabilityMode.ALWAYS_ON -> TimeBlock(0, 1440)
        AvailabilityMode.ON -> TimeBlock(day.openMin ?: 0, day.closeMin ?: 1440)
    }

    private fun violation(
        block: TimeBlock,
        win: TimeBlock?,
        existing: List<TimeBlock>,
        nowMin: Int?
    ): ConflictReason? {
        if (win == null) return ConflictReason.DAY_OFF
        if (nowMin != null && block.startMin < nowMin) return ConflictReason.PAST_TIME
        if (block.startMin < win.startMin || block.endMin > win.endMin) return ConflictReason.OUTSIDE_HOURS
        if (existing.any { overlaps(block, it) }) return ConflictReason.OVERLAP
        return null
    }

    /**
     * @param nowMin: isi menit sekarang (WIB) jika tanggal yang dipilih = hari ini, selain itu null.
     */
    fun check(
        requestedStartMin: Int,
        durationMin: Int,
        day: DayAvailability,
        existing: List<TimeBlock>,
        nowMin: Int?
    ): SlotCheckResult {
        val block = blockFor(requestedStartMin, durationMin)
        val win = window(day)
        val reason = violation(block, win, existing, nowMin)
        if (reason == null) return SlotCheckResult(true, null, block, emptyList())
        if (reason == ConflictReason.DAY_OFF) return SlotCheckResult(false, reason, block, emptyList())

        val grid = (0 until 1440 step BantuinConfig.SLOT_GRID_MIN).toList()
        val candidates = (existing.map { it.endMin } + grid + listOfNotNull(win?.startMin))
            .distinct().sorted().filter { it > requestedStartMin }
        val alts = candidates.filter { violation(blockFor(it, durationMin), win, existing, nowMin) == null }
            .take(BantuinConfig.MAX_ALTERNATIVE_SLOTS)

        return SlotCheckResult(false, reason, block, alts)
    }
}
