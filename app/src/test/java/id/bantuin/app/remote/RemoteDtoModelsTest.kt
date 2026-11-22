package id.bantuin.app.remote

import id.bantuin.app.data.remote.dto.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDtoModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    @Test
    fun testProfileDtoSerialization() {
        val dto = ProfileDto(
            id = "c0000000-0000-0000-0000-000000000001",
            fullName = "Rani Wijaya",
            phone = "081234567890",
            roles = listOf("customer")
        )
        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<ProfileDto>(serialized)
        assertEquals(dto.id, deserialized.id)
        assertEquals(dto.fullName, deserialized.fullName)
        assertEquals("customer", deserialized.roles.first())
    }

    @Test
    fun testProviderProfileDtoSerialization() {
        val dto = ProviderProfileDto(
            userId = "00000000-0000-0000-0000-000000000001",
            bio = "Teknisi AC Berlisensi BNSP",
            address = "Jl. Kaliurang No. 14, Malang",
            lat = -7.9650,
            lng = 112.6320,
            ratingAvg = 4.90,
            completionRate = 0.9900,
            trustScore = 92.00,
            txCount30d = 184,
            flagAnomaly = false,
            badge = "TRUSTED"
        )
        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<ProviderProfileDto>(serialized)
        assertEquals(dto.userId, deserialized.userId)
        assertEquals(92.00, deserialized.trustScore, 0.01)
        assertEquals("TRUSTED", deserialized.badge)
    }

    @Test
    fun testBookingDtoSerialization() {
        val dto = BookingDto(
            id = "20000000-0000-0000-0000-000000000001",
            customerId = "c0000000-0000-0000-0000-000000000001",
            providerId = "00000000-0000-0000-0000-000000000001",
            serviceId = "10000000-0000-0000-0000-000000000001",
            startAt = "2026-11-22T08:00:00+07:00",
            endAt = "2026-11-22T09:30:00+07:00",
            complexity = "SEDANG",
            isUrgent = false,
            distanceKm = 1.8,
            baseFare = 65000,
            baseCost = 68600,
            surgeApplied = false,
            totalCost = 75500,
            customerLat = -7.9797,
            customerLng = 112.6304,
            notes = "AC menetes",
            status = "PENDING",
            expiresAt = "2026-11-22T08:15:00+07:00"
        )
        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<BookingDto>(serialized)
        assertEquals("PENDING", deserialized.status)
        assertEquals(75500, deserialized.totalCost)
    }

    @Test
    fun testRpcParamsSerialization() {
        val lockParams = AcquireSlotLockRpcParams(
            providerId = "p01",
            startAt = "2026-11-22T08:00:00+07:00",
            endAt = "2026-11-22T09:30:00+07:00"
        )
        val lockJson = json.encodeToString(lockParams)
        assertTrue(lockJson.contains("p_provider"))
        assertTrue(lockJson.contains("p_start"))

        val statusParams = UpdateBookingStatusRpcParams(
            bookingId = "b01",
            newStatus = "DIKONFIRMASI"
        )
        val statusJson = json.encodeToString(statusParams)
        assertTrue(statusJson.contains("p_booking"))
        assertTrue(statusJson.contains("DIKONFIRMASI"))
    }
}
