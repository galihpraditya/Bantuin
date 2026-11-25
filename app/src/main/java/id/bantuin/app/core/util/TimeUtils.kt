package id.bantuin.app.core.util

import id.bantuin.app.core.config.BantuinConfig
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

object TimeUtils {
    val WIB_ZONE = TimeZone.of(BantuinConfig.TIMEZONE_ID)

    fun formatMinuteOfDay(minuteOfDay: Int, includeZone: Boolean = true): String {
        val normalized = ((minuteOfDay % 1440) + 1440) % 1440
        val hours = normalized / 60
        val minutes = normalized % 60
        val base = String.format(Locale.US, "%02d:%02d", hours, minutes)
        return if (includeZone) "$base WIB" else base
    }

    fun parseMinuteOfDay(hour: Int, minute: Int): Int {
        return hour * 60 + minute
    }

    fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60.0
        return if (durationMinutes % 60 == 0) {
            "$durationMinutes Menit (${durationMinutes / 60} jam)"
        } else {
            String.format(Locale.US, "%d Menit (%.1f jam)", durationMinutes, hours)
        }
    }

    fun formatInstantToTime(instant: Instant): String {
        val ldt = instant.toLocalDateTime(WIB_ZONE)
        return String.format(Locale.US, "%02d:%02d WIB", ldt.hour, ldt.minute)
    }

    fun formatInstantToDate(instant: Instant): String {
        val ldt = instant.toLocalDateTime(WIB_ZONE)
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Ags", "Sep", "Okt", "Nov", "Des"
        )
        return "${ldt.dayOfMonth} ${monthNames[ldt.monthNumber - 1]} ${ldt.year}"
    }

    fun formatCountdown(remainingSeconds: Long): String {
        val safeSeconds = remainingSeconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val seconds = safeSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun toWibMinutesOfDay(instant: Instant): Int {
        val ldt = instant.toLocalDateTime(WIB_ZONE)
        return ldt.hour * 60 + ldt.minute
    }

    fun toWibDateIso(instant: Instant): String {
        val ldt = instant.toLocalDateTime(WIB_ZONE)
        return String.format(Locale.US, "%04d-%02d-%02d", ldt.year, ldt.monthNumber, ldt.dayOfMonth)
    }

    fun todayIso(): String {
        val now = kotlinx.datetime.Clock.System.now()
        val ldt = now.toLocalDateTime(WIB_ZONE)
        return String.format(Locale.US, "%04d-%02d-%02d", ldt.year, ldt.monthNumber, ldt.dayOfMonth)
    }

    fun toIsoString(dateIso: String, minuteOfDay: Int): String {
        val dayOffset = Math.floorDiv(minuteOfDay, 1440)
        val minuteInDay = Math.floorMod(minuteOfDay, 1440)
        val targetDate = if (dayOffset == 0) {
            dateIso
        } else {
            java.time.LocalDate.parse(dateIso).plusDays(dayOffset.toLong()).toString()
        }
        val hours = minuteInDay / 60
        val minutes = minuteInDay % 60
        return String.format(Locale.US, "%sT%02d:%02d:00+07:00", targetDate, hours, minutes)
    }
}
