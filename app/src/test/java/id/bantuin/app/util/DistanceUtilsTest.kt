package id.bantuin.app.util

import id.bantuin.app.core.util.DistanceUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUtilsTest {
    @Test
    fun formatKm_usesOneDecimalAndKmUnit() {
        assertEquals("2.5 km", DistanceUtils.formatKm(2.5))
        assertEquals("0.0 km", DistanceUtils.formatKm(0.0))
        assertEquals("17.1 km", DistanceUtils.formatKm(17.06))
    }

    @Test
    fun formatWithAddress_combinesDistanceAndHumanAddress() {
        assertEquals("2.5 km · Jl. Ijen No. 25", DistanceUtils.formatWithAddress(2.5, "Jl. Ijen No. 25"))
        assertEquals("2.5 km", DistanceUtils.formatWithAddress(2.5, null))
        assertEquals("Jl. Ijen No. 25", DistanceUtils.formatWithAddress(0.0, "Jl. Ijen No. 25"))
        assertEquals("Lokasi layanan", DistanceUtils.formatWithAddress(0.0, ""))
    }
}
