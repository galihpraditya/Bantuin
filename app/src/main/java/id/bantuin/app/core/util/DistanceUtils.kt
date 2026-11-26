package id.bantuin.app.core.util

import java.util.Locale

object DistanceUtils {
    fun formatKm(distanceKm: Double): String =
        String.format(Locale.US, "%.1f km", distanceKm)
}
