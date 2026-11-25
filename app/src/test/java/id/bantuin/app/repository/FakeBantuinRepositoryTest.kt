package id.bantuin.app.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.model.PayoutChannelType
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
        // Pelanggan lain harus melihat blok [480, 600) termasuk buffer 30 menit saat lock masih aktif
        val sessionStore = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_demo_rani",
                fullName = "Rani Wijaya",
                email = "pelanggan1@bantuin.test",
                phone = "081234567890",
                roles = setOf("customer")
            )
        )
        val sharedRepo = FakeBantuinRepository(sessionStore = sessionStore)
        val lockRes = sharedRepo.acquireLock("p01", "2026-11-28", 480, 600)
        assertTrue(lockRes is AppResult.Success)
        val lock = (lockRes as AppResult.Success).data
        assertEquals("2026-11-28", lock.dateIso)
        assertEquals(600, lock.endMin)

        sessionStore.saveSession(
            UserProfile(
                id = "user_demo_dimas",
                fullName = "Dimas Arya",
                email = "pelanggan2@bantuin.test",
                phone = "081234567891",
                roles = setOf("customer")
            )
        )
        val busyForOtherUser = (sharedRepo.busyBlocks("p01", "2026-11-28") as AppResult.Success).data
        assertTrue(busyForOtherUser.any { it.startMin == 480 && it.endMin == 600 })

        sessionStore.saveSession(
            UserProfile(
                id = "user_demo_rani",
                fullName = "Rani Wijaya",
                email = "pelanggan1@bantuin.test",
                phone = "081234567890",
                roles = setOf("customer")
            )
        )

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
        val bookingId = (sharedRepo.createBooking(draft) as AppResult.Success).data
        val busyAfterBooking = (sharedRepo.busyBlocks("p01", "2026-11-28") as AppResult.Success).data
        assertTrue(busyAfterBooking.any { it.startMin == 480 && it.endMin == 600 })
        sharedRepo.updateStatus(bookingId, BookingStatus.DIKONFIRMASI)
        sharedRepo.updateStatus(bookingId, BookingStatus.DALAM_PENGERJAAN)
        sharedRepo.updateStatus(bookingId, BookingStatus.SELESAI)

        val eventsRes = sharedRepo.events(bookingId) as AppResult.Success
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

    @Test
    fun updateCustomerProfile_updatesNamePhoneDefaultAddressAndLocation() = runBlocking {
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
        val loc = GeoPoint(-7.9666, 112.6326)
        val res = customerRepo.updateCustomerProfile(
            fullName = "Rani Putri",
            phone = "081299988877",
            defaultAddress = "Jl. Ijen No. 12, Malang",
            defaultLocation = loc
        )
        assertTrue(res is AppResult.Success)
        val user = customerRepo.currentUser().first()
        assertEquals("Rani Putri", user?.fullName)
        assertEquals("081299988877", user?.phone)
        assertEquals("Jl. Ijen No. 12, Malang", user?.defaultAddress)
        assertEquals(loc, user?.defaultLocation)
    }

    @Test
    fun qrisAndCashCompletion_respectsAvailableBalanceAndCombinedRevenueRules() = runBlocking {
        val sharedSession = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_demo_rani",
                fullName = "Rani Wijaya",
                email = "pelanggan1@bantuin.test",
                phone = "081234567890",
                roles = setOf("customer")
            )
        )
        val sharedRepo = FakeBantuinRepository(sessionStore = sharedSession)
        val provider = FakeData.providers.first { it.providerId == "p02" }
        val service = FakeData.services.first { it.providerId == "p02" }

        val qrisDraft = OrderDraft(
            categoryId = service.categoryId,
            provider = provider,
            service = service,
            dateIso = "2026-11-28",
            startMin = 540,
            durationMin = 60,
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Ijen",
            distanceKm = 1.0,
            paymentMethod = PaymentMethod.QRIS
        )
        val qrisId = (sharedRepo.createBooking(qrisDraft) as AppResult.Success).data
        val createdQris = sharedRepo.observeBooking(qrisId).first()
        assertEquals(PaymentStatus.WAITING_PAYMENT, createdQris?.paymentStatus)
        assertTrue(sharedRepo.confirmQrisPayment(qrisId) is AppResult.Success)
        assertEquals(PaymentStatus.PAID, sharedRepo.observeBooking(qrisId).first()?.paymentStatus)

        sharedRepo.updateStatus(qrisId, BookingStatus.DIKONFIRMASI)
        sharedRepo.updateStatus(qrisId, BookingStatus.DALAM_PENGERJAAN)
        assertTrue(sharedRepo.completeBooking(qrisId, "Berhasil dibersihkan", false) is AppResult.Success)

        val cashDraft = qrisDraft.copy(
            startMin = 720,
            paymentMethod = PaymentMethod.CASH
        )
        val cashId = (sharedRepo.createBooking(cashDraft) as AppResult.Success).data
        sharedRepo.updateStatus(cashId, BookingStatus.DIKONFIRMASI)
        sharedRepo.updateStatus(cashId, BookingStatus.DALAM_PENGERJAAN)
        val blockedCash = sharedRepo.completeBooking(cashId, "Selesai", cashCollectedConfirmed = false)
        assertTrue(blockedCash is AppResult.Error)
        assertTrue(sharedRepo.completeBooking(cashId, "Tunai diterima", cashCollectedConfirmed = true) is AppResult.Success)

        val analytics = sharedRepo.observeProviderRevenue("p02").first()
        assertEquals(service.baseFare * 2, analytics.totalCombinedRevenue)
        assertEquals(service.baseFare, analytics.qrisGrossRevenue)
        assertEquals(service.baseFare, analytics.cashRevenue)
        assertEquals(service.baseFare, analytics.availableBalance)
    }

    @Test
    fun requestPayout_validatesMinAmountAndAvailableBalance_andDeductsOnSuccess() = runBlocking {
        val sharedSession = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_demo_rani",
                fullName = "Rani Wijaya",
                email = "pelanggan1@bantuin.test",
                phone = "081234567890",
                roles = setOf("customer")
            )
        )
        val sharedRepo = FakeBantuinRepository(sessionStore = sharedSession)
        val provider = FakeData.providers.first { it.providerId == "p02" }
        val service = FakeData.services.first { it.providerId == "p02" }

        val qrisDraft = OrderDraft(
            categoryId = service.categoryId,
            provider = provider,
            service = service,
            dateIso = "2026-11-29",
            startMin = 540,
            durationMin = 60,
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Ijen",
            distanceKm = 1.0,
            paymentMethod = PaymentMethod.QRIS
        )
        val qrisId = (sharedRepo.createBooking(qrisDraft) as AppResult.Success).data
        sharedRepo.confirmQrisPayment(qrisId)
        sharedRepo.updateStatus(qrisId, BookingStatus.DIKONFIRMASI)
        sharedRepo.updateStatus(qrisId, BookingStatus.DALAM_PENGERJAAN)
        sharedRepo.completeBooking(qrisId, null, false)

        val tooSmall = sharedRepo.requestPayout("p02", 10_000, PayoutChannelType.BANK, "BCA", "12345", "Dewi")
        assertTrue(tooSmall is AppResult.Error)

        val available = sharedRepo.observeProviderRevenue("p02").first().availableBalance
        val tooLarge = sharedRepo.requestPayout("p02", available + 5_000, PayoutChannelType.BANK, "BCA", "12345", "Dewi")
        assertTrue(tooLarge is AppResult.Error)

        val valid = sharedRepo.requestPayout("p02", 30_000, PayoutChannelType.BANK, "BCA", "12345", "Dewi")
        assertTrue(valid is AppResult.Success)
        val updatedAnalytics = sharedRepo.observeProviderRevenue("p02").first()
        assertEquals(available - 30_000, updatedAnalytics.availableBalance)
        assertEquals(1, sharedRepo.observePayoutHistory("p02").first().size)
    }

    @Test
    fun sendMessage_rejectsBlank_andEmitsMessagesAndThreadSummary() = runBlocking {
        val sharedRepo = FakeBantuinRepository(
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

        val blankRes = sharedRepo.sendMessage("b01", "   ")
        assertTrue(blankRes is AppResult.Error)

        val sendRes = sharedRepo.sendMessage("b01", "Pak, mohon bawa tangga ya.")
        assertTrue(sendRes is AppResult.Success)

        val messages = sharedRepo.observeMessages("b01").first()
        assertTrue(messages.any { it.text == "Pak, mohon bawa tangga ya." })

        val threads = sharedRepo.observeChatThreads("customer").first()
        assertTrue(threads.any { it.bookingId == "b01" })
    }
}
