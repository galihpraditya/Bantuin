package id.bantuin.app.repository

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.data.remote.SupabaseClientProvider
import id.bantuin.app.data.remote.dto.CreateBookingRpcParams
import id.bantuin.app.data.repository.SupabaseBantuinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseBantuinRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var supabaseRepo: SupabaseBantuinRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionStore = InMemorySessionStore()
        supabaseRepo = SupabaseBantuinRepository(
            client = SupabaseClientProvider.client,
            sessionStore = sessionStore
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mapSupabaseError_mapsAllPrdRpcCodesToIndonesianMessages() {
        assertEquals(
            "Slot baru saja diambil, pilih jam lain",
            supabaseRepo.mapSupabaseError(IllegalStateException("SLOT_CONFLICT"), "default").message
        )
        assertEquals(
            "Slot baru saja diambil, pilih jam lain",
            supabaseRepo.mapSupabaseError(IllegalStateException("SLOT_LOCKED"), "default").message
        )
        assertEquals(
            "Slot baru saja diambil, pilih jam lain",
            supabaseRepo.mapSupabaseError(IllegalStateException("duplicate key 23P01"), "default").message
        )
        assertEquals(
            "Waktu penahanan slot habis, pilih jadwal lagi",
            supabaseRepo.mapSupabaseError(IllegalStateException("LOCK_EXPIRED"), "default").message
        )
        assertEquals(
            "Pesanan sudah kedaluwarsa",
            supabaseRepo.mapSupabaseError(IllegalStateException("EXPIRED"), "default").message
        )
        assertEquals(
            "Aksi tidak diizinkan",
            supabaseRepo.mapSupabaseError(IllegalStateException("INVALID_TRANSITION"), "default").message
        )
        assertEquals(
            "Aksi tidak diizinkan",
            supabaseRepo.mapSupabaseError(IllegalStateException("FORBIDDEN"), "default").message
        )
        assertEquals(
            "Tidak bisa memesan layanan sendiri",
            supabaseRepo.mapSupabaseError(IllegalStateException("SELF_BOOKING"), "default").message
        )
        assertEquals(
            "Sesi login berakhir, silakan masuk kembali",
            supabaseRepo.mapSupabaseError(IllegalStateException("UNAUTHENTICATED"), "default").message
        )
    }

    @Test
    fun mapSupabaseError_mapsPostgrestWriteViolations() {
        assertEquals(
            "Tidak memiliki izin mengakses data. Periksa hak akses akun Anda lalu coba lagi.",
            supabaseRepo.mapSupabaseError(IllegalStateException("42501 permission denied"), "default").message
        )
        assertEquals(
            "Format jadwal tidak valid. Periksa jam buka dan jam tutup Anda.",
            supabaseRepo.mapSupabaseError(IllegalStateException("new row violates check constraint 23514"), "default").message
        )
        assertEquals(
            "Akun mitra belum terdaftar di database.",
            supabaseRepo.mapSupabaseError(IllegalStateException("foreign key violation 23503"), "default").message
        )
        assertEquals(
            "Akun mitra tidak valid. Silakan masuk kembali.",
            supabaseRepo.mapSupabaseError(IllegalStateException("invalid input syntax for type uuid 22P02"), "default").message
        )
    }

    @Test
    fun mapSupabaseError_mapsCreateBookingServerErrors() {
        assertEquals(
            "Fungsi pemesanan di server belum sesuai. Terapkan ulang migrasi 006 lalu coba lagi.",
            supabaseRepo.mapSupabaseError(
                IllegalStateException("PGRST202 Could not find the function public.create_booking"),
                "Gagal membuat pesanan"
            ).message
        )
        assertEquals(
            "Jadwal pesanan tidak valid. Silakan pilih jadwal lain.",
            supabaseRepo.mapSupabaseError(IllegalStateException("INVALID_SCHEDULE"), "Gagal membuat pesanan").message
        )
        assertEquals(
            "Layanan tidak tersedia atau sudah dinonaktifkan.",
            supabaseRepo.mapSupabaseError(IllegalStateException("INVALID_SERVICE"), "Gagal membuat pesanan").message
        )
    }

    @Test
    fun createBookingRpcParams_alwaysSerializesNotesEvenWhenBlank() {
        // Supabase-kt memakai Json { encodeDefaults = false }; field non-nullable
        // dengan default tetap dikirim sehingga p_notes tidak hilang dari payload.
        val json = Json { encodeDefaults = false }
        val payload = json.encodeToString(
            CreateBookingRpcParams(
                serviceId = "service-1",
                startAt = "2026-11-26T10:00:00+07:00",
                endAt = "2026-11-26T11:30:00+07:00",
                complexity = "SEDANG",
                isUrgent = false,
                distanceKm = 2.0,
                baseFare = 100000,
                baseCost = 120000,
                surgeApplied = false,
                totalCost = 120000,
                customerLat = -7.95,
                customerLng = 112.63,
                notes = "",
                paymentMethod = "CASH"
            )
        )
        assertTrue(payload.contains("\"p_notes\":\"\""))
        assertTrue(payload.contains("\"p_start_at\""))
        assertTrue(payload.contains("\"p_end_at\""))
        assertTrue(!payload.contains("p_lock_id"))
    }

    @Test
    fun mapSupabaseError_mapsNetworkTimeoutCleanly() {
        val res = supabaseRepo.mapSupabaseError(RuntimeException("Socket timeout while connecting"), "Gagal")
        assertTrue(res.message.contains("Koneksi", ignoreCase = true))
    }

    @Test
    fun timeUtils_andMidnightBlockParsing_handle1440RolloverCorrectly() {
        val endMidnightIso = TimeUtils.toIsoString("2026-11-25", 1440)
        assertEquals("2026-11-26T00:00:00+07:00", endMidnightIso)
        assertEquals(
            1440,
            supabaseRepo.parseIsoToWibMinutesOnDate("2026-11-25T17:00:00Z", "2026-11-25")
        )
        assertEquals(
            0,
            supabaseRepo.parseIsoToWibMinutesOnDate("2026-11-24T17:00:00Z", "2026-11-25")
        )
        assertEquals(30, BantuinConfig.TRAVEL_BUFFER_MIN)
    }
}
