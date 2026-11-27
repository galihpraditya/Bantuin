package id.bantuin.app.location

import id.bantuin.app.core.location.LocationHelper
import id.bantuin.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocationHelperTest {
    @Test
    fun coordinateFallback_doesNotExposeLatLng() {
        val text = LocationHelper.formatCoordinateFallback(GeoPoint(-7.9797, 112.6304))
        assertEquals("Lokasi terpilih di peta", text)
        assertFalse(text.contains("7.97"))
        assertFalse(text.contains("112.63"))
    }
}
