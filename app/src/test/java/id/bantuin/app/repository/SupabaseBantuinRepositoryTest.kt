package id.bantuin.app.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.remote.SupabaseClientProvider
import id.bantuin.app.data.repository.SupabaseBantuinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
    fun mapSupabaseError_mapsNetworkTimeoutCleanly() {
        val res = supabaseRepo.mapSupabaseError(RuntimeException("Socket timeout while connecting"), "Gagal")
        assertTrue(res.message.contains("Koneksi", ignoreCase = true))
    }
}
