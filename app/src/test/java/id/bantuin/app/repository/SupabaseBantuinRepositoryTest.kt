package id.bantuin.app.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.remote.SupabaseClientProvider
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.data.repository.SupabaseBantuinRepository
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.OrderDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
    private lateinit var fakeRepo: FakeBantuinRepository
    private lateinit var supabaseRepo: SupabaseBantuinRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionStore = InMemorySessionStore()
        fakeRepo = FakeBantuinRepository(sessionStore = sessionStore)
        supabaseRepo = SupabaseBantuinRepository(
            client = SupabaseClientProvider.client,
            sessionStore = sessionStore,
            fallback = fakeRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFallbackCategories() = runBlocking {
        val result = supabaseRepo.categories()
        assertTrue(result is AppResult.Success)
        val categories = (result as AppResult.Success).data
        assertEquals(6, categories.size)
        assertEquals("Teknisi Listrik", categories.first().name)
    }

    @Test
    fun testFallbackCandidates() = runBlocking {
        val result = supabaseRepo.candidates(2)
        assertTrue(result is AppResult.Success)
        val candidates = (result as AppResult.Success).data
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.any { it.providerId == "p01" })
    }

    @Test
    fun testFallbackCreateBooking() = runBlocking {
        val provider = FakeData.providers.first()
        val service = FakeData.services.first()
        val draft = OrderDraft(
            categoryId = 2,
            provider = provider,
            service = service,
            dateIso = "2026-11-22",
            startMin = 480,
            durationMin = 90,
            complexity = Complexity.RINGAN,
            isUrgent = false,
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Alun-alun Malang",
            distanceKm = 1.8
        )
        val result = supabaseRepo.createBooking(draft)
        assertTrue(result is AppResult.Success)
        val bookingId = (result as AppResult.Success).data
        assertTrue(bookingId.isNotBlank())
    }
}
