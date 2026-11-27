package id.bantuin.app.core.util

import java.util.Locale

object DistanceUtils {
    fun formatKm(distanceKm: Double): String =
        String.format(Locale.US, "%.1f km", distanceKm)

    /** Label lokasi untuk card: jarak + alamat bila keduanya tersedia. */
    fun formatWithAddress(distanceKm: Double, address: String?): String {
        val cleanAddress = address?.trim().orEmpty()
        return when {
            distanceKm > 0.0 && cleanAddress.isNotBlank() -> "${formatKm(distanceKm)} · $cleanAddress"
            distanceKm > 0.0 -> formatKm(distanceKm)
            cleanAddress.isNotBlank() -> cleanAddress
            else -> "Lokasi layanan"
        }
    }
}
