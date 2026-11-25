package id.bantuin.app.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBantuinRepositoryTest {
    private lateinit var repo: FakeBantuinRepository

    @Before
    fun setUp() {
        repo = FakeBantuinRepository(sessionStore = InMemorySessionStore(initialUser = null))
    }

    @Test
    fun signIn_withWrongCredentials_isRejected() = runBlocking {
        val result = repo.signIn("pelanggan1@bantuin.test", "salah")

        assertTrue(result is AppResult.Error)
        assertEquals(null, repo.currentUser().firstOrNull())
    }

    @Test
    fun signIn_withMitraCredentials_returnsMitraProfile() = runBlocking {
        val result = repo.signIn("mitra01@bantuin.test", "Demo12345!")

        assertTrue(result is AppResult.Success)
        val user = repo.currentUser().firstOrNull()
        assertEquals("p01", user?.id)
        assertEquals("Budi Santoso, S.T.", user?.fullName)
        assertEquals(setOf("provider"), user?.roles)
    }

    @Test
    fun submitReview_triggersRecalculateMetrics_andUpdatesProviderList() = runBlocking {
        val initialResult = repo.candidates(2)
        assertTrue(initialResult is AppResult.Success<List<ProviderCandidate>>)
        val initialCandidate = (initialResult as AppResult.Success<List<ProviderCandidate>>).data.first { it.providerId == "p01" }
        val initialRating = initialCandidate.ratingAvg ?: 0.0

        repo.submitReview(
            bookingId = "b01",
            providerId = "p01",
            rating = 5,
            comment = "Layanan istimewa dan cepat",
            tags = listOf("Cepat", "Rapi")
        )

        val updatedResult = repo.candidates(2)
        assertTrue(updatedResult is AppResult.Success<List<ProviderCandidate>>)
        val updatedCandidate = (updatedResult as AppResult.Success<List<ProviderCandidate>>).data.first { it.providerId == "p01" }
        assertNotNull(updatedCandidate.ratingAvg)
        assertTrue(updatedCandidate.ratingAvg!! >= initialRating)
    }

    @Test
    fun updateProviderProfile_updatesLocationAndBio() = runBlocking {
        val newLoc = GeoPoint(-7.9500, 112.6200)
        val res = repo.updateProviderProfile(
            providerId = "p01",
            name = "Budi Santoso Baru",
            bio = "Teknisi AC bersertifikat 10 tahun",
            location = newLoc,
            address = "Jl. Sukarno Hatta No. 45, Malang",
            phone = "081299998888"
        )
        assertTrue(res is AppResult.Success<Unit>)

        val candidateResult = repo.candidates(2)
        assertTrue(candidateResult is AppResult.Success<List<ProviderCandidate>>)
        val candidate = (candidateResult as AppResult.Success<List<ProviderCandidate>>).data.first { it.providerId == "p01" }
        assertEquals("Budi Santoso Baru", candidate.name)
        assertEquals("Teknisi AC bersertifikat 10 tahun", candidate.bio)
        assertEquals("Jl. Sukarno Hatta No. 45, Malang", candidate.address)
        assertEquals(newLoc.lat, candidate.location.lat, 0.0001)
    }

    @Test
    fun candidates_excludesLoggedInSelfProvider() = runBlocking {
        val mitraRepo = FakeBantuinRepository(
            sessionStore = InMemorySessionStore(
                initialRole = "customer",
                initialUser = UserProfile(
                    id = "p01",
                    fullName = "Budi Santoso, S.T.",
                    email = "mitra01@bantuin.test",
                    phone = "081200000001",
                    roles = setOf("customer", "provider")
                )
            )
        )
        val result = mitraRepo.candidates(2) as AppResult.Success
        assertFalse(result.data.any { it.providerId == "p01" })
    }

    @Test
    fun reviewsOf_filtersByProviderId() = runBlocking {
        val r07 = repo.reviewsOf("p07") as AppResult.Success
        assertTrue("Mitra baru tanpa ulasan tidak boleh menampilkan ulasan mitra lain", r07.data.isEmpty())
    }

    @Test
    fun providerProfile_returnsAnyCategoryProviderDirectly() = runBlocking {
        val p04 = repo.providerProfile("p04") as AppResult.Success
        assertNotNull(p04.data)
        assertEquals("Pak Sugeng W.", p04.data?.name)
    }

    @Test
    fun acquireLock_recordsSelectedDateIso_andEventsTrackStatusTransitions() = runBlocking {
        val customerRepo = FakeBantuinRepository(
            sessionStore = InMemorySessionStore(
                initialRole = "customer",
                initialUser = UserProfile(
                    id = "user_demo_rani",
                    fullName = "Rani Wijaya",
                    email = "pelanggan1@bantuin.test",
                    phone = "081234567890",
                    roles = setOf("customer")
                )
            )
        )
        val lockRes = customerRepo.acquireLock("p01", "2026-11-28", 480, 570)
        assertTrue(lockRes is AppResult.Success)
        val lock = (lockRes as AppResult.Success).data
        assertEquals("2026-11-28", lock.dateIso)

        val draft = OrderDraft(
            categoryId = 2,
            provider = FakeData.providers.first { it.providerId == "p01" },
            service = FakeData.services.first { it.providerId == "p01" },
            dateIso = "2026-11-28",
            startMin = 480,
            durationMin = 90,
            complexity = Complexity.SEDANG,
            isUrgent = false,
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Alun-alun Malang",
            distanceKm = 1.8,
            lockInfo = lock
        )
        val bookingId = (customerRepo.createBooking(draft) as AppResult.Success).data
        val busyAfterBooking = (customerRepo.busyBlocks("p01", "2026-11-28") as AppResult.Success).data
        assertTrue(busyAfterBooking.any { it.startMin == 480 && it.endMin == 600 })
        customerRepo.updateStatus(bookingId, BookingStatus.DIKONFIRMASI)
        customerRepo.updateStatus(bookingId, BookingStatus.DALAM_PENGERJAAN)
        customerRepo.updateStatus(bookingId, BookingStatus.SELESAI)

        val eventsRes = customerRepo.events(bookingId) as AppResult.Success
        assertEquals(
            listOf(
                BookingStatus.PENDING,
                BookingStatus.DIKONFIRMASI,
                BookingStatus.DALAM_PENGERJAAN,
                BookingStatus.SELESAI
            ),
            eventsRes.data.map { it.status }
        )
    }

    @Test
    fun enableProviderRole_addsProviderRoleToCurrentUser() = runBlocking {
        val customerRepo = FakeBantuinRepository(
            sessionStore = InMemorySessionStore(
                initialRole = "customer",
                initialUser = UserProfile(
                    id = "user_demo_dimas",
                    fullName = "Dimas Arya",
                    email = "pelanggan2@bantuin.test",
                    phone = "081234567891",
                    roles = setOf("customer")
                )
            )
        )
        val res = customerRepo.enableProviderRole(
            address = "Jl. Soekarno Hatta, Malang",
            location = GeoPoint(-7.9467, 112.6156),
            bio = "Teknisi serba bisa"
        )
        assertTrue(res is AppResult.Success)
        val updatedUser = customerRepo.currentUser().first()
        assertTrue(updatedUser?.roles?.contains("provider") == true)
        val profile = (customerRepo.providerProfile("user_demo_dimas") as AppResult.Success).data
        assertNotNull(profile)
        assertEquals(ProviderBadge.NEW, profile?.badge)
    }
}
