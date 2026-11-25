package id.bantuin.app.viewmodel

import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.customer.estimate.EstimateViewModel
import id.bantuin.app.ui.customer.home.CustomerHomeViewModel
import id.bantuin.app.ui.customer.notifications.CustomerNotificationsViewModel
import id.bantuin.app.ui.customer.notifications.NotificationType
import id.bantuin.app.ui.customer.orderdetail.OrderDetailViewModel
import id.bantuin.app.ui.customer.orders.CustomerOrdersViewModel
import id.bantuin.app.ui.customer.profile.CustomerProfileViewModel
import id.bantuin.app.ui.customer.providerdetail.ProviderDetailViewModel
import id.bantuin.app.ui.customer.recommendation.RecommendationViewModel
import id.bantuin.app.ui.customer.schedule.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBantuinRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBantuinRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun customerHomeViewModel_loadsCategoriesAndUpdatesLocation() = runTest {
        val vm = CustomerHomeViewModel(repository, repository, repository, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.categories.isNotEmpty())
        assertEquals("Rani Wijaya", vm.uiState.value.user?.fullName)

        val newPoint = GeoPoint(-7.9467, 112.6156)
        vm.updateLocation("Jl. Suhat", newPoint)

        assertEquals("Jl. Suhat", vm.uiState.value.customerAddress)
        assertEquals(newPoint, vm.uiState.value.customerLocation)
    }

    @Test
    fun customerHomeViewModel_hydratesDefaultLocationAndActiveOrderSummary() = runTest {
        val defaultLoc = GeoPoint(-7.9512, 112.6145)
        val sessionStore = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_demo_rani",
                fullName = "Rani Wijaya",
                email = "pelanggan1@bantuin.test",
                phone = "081234567890",
                roles = setOf("customer"),
                defaultAddress = "Jl. Veteran No. 10, Malang",
                defaultLocation = defaultLoc
            )
        )
        val repo = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = CustomerHomeViewModel(repo, repo, repo, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Jl. Veteran No. 10, Malang", vm.uiState.value.customerAddress)
        assertEquals(defaultLoc, vm.uiState.value.customerLocation)
        assertNotNull(vm.uiState.value.activeOrderSummary)
        assertTrue(vm.uiState.value.unreadChatCount >= 0)
    }

    @Test
    fun recommendationViewModel_filtersAndRanksCandidates() = runTest {
        val vm = RecommendationViewModel(repository)
        vm.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialCount = vm.uiState.value.filteredList.size
        assertTrue(initialCount > 0)

        // Terapkan filter jarak 5km
        vm.setMaxDistanceFilter(5.0)
        assertTrue(vm.uiState.value.filteredList.all { it.distanceKm <= 5.0 })

        // Terapkan filter rating 4.8+
        vm.setMinRatingFilter(4.8)
        assertTrue(vm.uiState.value.filteredList.all { (it.candidate.ratingAvg ?: 0.0) >= 4.8 })
    }

    @Test
    fun scheduleViewModel_selectsSlotAndAcquiresLock() = runTest {
        val vm = ScheduleViewModel(repository, repository)
        val provider = FakeData.providers.first { it.providerId == "p01" }
        val service = FakeData.services.first { it.providerId == "p01" }

        vm.initialize(provider = provider, service = service, distanceKm = 1.8)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(7, vm.uiState.value.dynamicDates.size)

        // Pilih slot jam bentrok -> harus ditolak saat mengunci
        vm.selectSlot(600) // 10:00 (bentrok dengan busyBlocks demo)
        assertTrue(vm.uiState.value.slotCheckResult != null)
        assertEquals(false, vm.uiState.value.slotCheckResult?.valid)

        var lockAcquired = false
        vm.acquireLockAndProceed { lockAcquired = true }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(!lockAcquired)
        assertNotNull(vm.uiState.value.errorMessage)

        // Pilih slot valid berikutnya (14:00 = 840, di luar blok 10:00-11:30 dan 12:00-13:45)
        vm.selectDate(1) // besok agar tidak terkena PAST_TIME pada waktu siang/malam test
        testDispatcher.scheduler.advanceUntilIdle()
        vm.selectSlot(840)
        vm.acquireLockAndProceed { lockInfo ->
            lockAcquired = true
            assertNotNull(lockInfo.lockId)
            assertEquals(vm.uiState.value.selectedDate.toString(), lockInfo.dateIso)
            // Wajib menyimpan end + 30 menit TRAVEL_BUFFER_MIN
            assertEquals(840 + service.durationMin + id.bantuin.app.core.config.BantuinConfig.TRAVEL_BUFFER_MIN, lockInfo.endMin)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(lockAcquired)
        assertNotNull(vm.uiState.value.lockInfo)
    }

    @Test
    fun scheduleViewModel_alwaysOnIncludesNightAnd30MinGridSlots() = runTest {
        val vm = ScheduleViewModel(repository, repository)
        val alwaysOnProvider = FakeData.providers.first { it.providerId == "p09" }
        val shortService = FakeData.services.first().copy(providerId = "p09", durationMin = 30)

        vm.initialize(provider = alwaysOnProvider, service = shortService, distanceKm = 1.0)
        vm.selectDate(1) // besok
        testDispatcher.scheduler.advanceUntilIdle()

        // Slot 23:00 (1380 + 30 + 30 = 1440) dan 05:30 (330) harus muncul pada grid 30 menit 24 jam
        val slotMins = vm.uiState.value.availableSlots.map { it.first }
        assertTrue(slotMins.contains(1380))
        assertTrue(slotMins.contains(330))
        assertTrue(slotMins.contains(30))
    }

    @Test
    fun estimateViewModel_calculatesDynamicFareAndConfirmsOrder() = runTest {
        val vm = EstimateViewModel(repository)
        val provider = FakeData.providers.first()
        val service = FakeData.services.first()

        vm.initialize(
            categoryId = 2,
            provider = provider,
            service = service,
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Alun-alun",
            scheduledDateIso = "2026-11-21",
            scheduledStartMin = 540,
            scheduledDurationMin = 90,
            lockInfo = null,
            distanceKm = 2.0
        )

        val baseTotal = vm.uiState.value.costEstimate?.total ?: 0
        assertTrue(baseTotal > 0)

        // Ubah kompleksitas ke BERAT
        vm.setComplexity(Complexity.BERAT)
        val heavyTotal = vm.uiState.value.costEstimate?.total ?: 0
        assertTrue(heavyTotal > baseTotal)

        var createdId: String? = null
        vm.confirmOrder { id -> createdId = id }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(createdId)
        assertEquals(createdId, vm.uiState.value.createdBookingId)
    }

    @Test
    fun orderDetailViewModel_observesAndSubmitsReview() = runTest {
        val vm = OrderDetailViewModel(repository, repository)
        vm.observeBooking("b01")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setRating(5)
        vm.setReviewComment("Sangat memuaskan")
        vm.toggleTag("Tepat Waktu")

        var reviewDone = false
        vm.submitReview { reviewDone = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(reviewDone)
        assertTrue(vm.uiState.value.reviewSubmitted)
    }

    @Test
    fun customerNotificationsViewModel_loadsNotificationsAndSyncsWithBookings() = runTest {
        val vm = CustomerNotificationsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.notifications.isNotEmpty())
        assertTrue(state.notifications.any { it.type == NotificationType.SYSTEM_INFO })
        assertTrue(state.notifications.any { it.id == "sys_notif_garansi" })
        assertTrue(state.notifications.any { it.id == "sys_notif_welcome" })
    }

    @Test
    fun customerHomeViewModel_updatesSearchQueryAndNearbyProviders() = runTest {
        val vm = CustomerHomeViewModel(repository, repository, repository, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.nearbyProviders.isNotEmpty())
        assertEquals("Lokasi default (ketuk untuk atur)", vm.uiState.value.locationSourceLabel)

        vm.onSearchQueryChange("Listrik")
        assertEquals("Listrik", vm.uiState.value.searchQuery)
        assertTrue(vm.uiState.value.filteredCategories.any { it.name.contains("Listrik", ignoreCase = true) })

        vm.updateLocation("Jl. Veteran Malang", GeoPoint(-7.9553, 112.6130), fromGps = true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Terdeteksi GPS akurat", vm.uiState.value.locationSourceLabel)
    }

    @Test
    fun providerDetailViewModel_fetchesProviderProfileWhenInitialProviderIsNull() = runTest {
        val vm = ProviderDetailViewModel(repository, repository)
        vm.loadProviderDetail(providerId = "p02", initialProvider = null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.provider)
        assertEquals("p02", vm.uiState.value.provider?.providerId)
        assertTrue(vm.uiState.value.services.isNotEmpty())
        assertTrue(vm.uiState.value.services.all { it.providerId == "p02" })
    }

    @Test
    fun estimateViewModel_releasesLockWhenRequestedAndHandlesExpiry() = runTest {
        val lock = (repository.acquireLock("p01", "2026-11-22", 840, 930) as id.bantuin.app.core.result.AppResult.Success).data
        val vm = EstimateViewModel(repository)
        vm.initialize(
            categoryId = 2,
            provider = FakeData.providers.first(),
            service = FakeData.services.first(),
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Ijen",
            scheduledDateIso = "2026-11-22",
            scheduledStartMin = 840,
            scheduledDurationMin = 90,
            lockInfo = lock,
            distanceKm = 1.5
        )

        vm.onLockExpired()
        assertTrue(vm.uiState.value.isLockExpired)
        assertNotNull(vm.uiState.value.errorMessage)

        vm.releaseLock()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, vm.uiState.value.lockInfo)
    }

    @Test
    fun orderDetailViewModel_loadsTimelineEventsAndRejectsIllegalCompletionAndLongReview() = runTest {
        val vm = OrderDetailViewModel(repository, repository)
        vm.observeBooking("b01")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.events.isNotEmpty())

        // Pelanggan tidak boleh menyelesaikan pesanan sendiri
        vm.updateStatus(BookingStatus.SELESAI)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            "Hanya mitra yang dapat menyelesaikan atau memperbarui status pengerjaan.",
            vm.uiState.value.errorMessage
        )

        // Komentar melebihi 300 karakter harus ditolak sebelum submit
        vm.setReviewComment("a".repeat(301))
        var reviewSubmitted = false
        vm.submitReview { reviewSubmitted = true }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(!reviewSubmitted)
        assertEquals("Komentar ulasan maksimal 300 karakter.", vm.uiState.value.errorMessage)
    }

    @Test
    fun customerOrdersViewModel_observesBookingsAndRunsExpirePending() = runTest {
        val vm = CustomerOrdersViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.orders.isNotEmpty())
        assertTrue(vm.uiState.value.hasSyncedPendingExpiry)
    }

    @Test
    fun customerProfileViewModel_loadsStatsValidatesAndSavesCustomerProfile() = runTest {
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
        val customerRepo = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = CustomerProfileViewModel(customerRepo, customerRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Rani Wijaya", vm.uiState.value.nameInput)
        assertTrue(vm.uiState.value.activeCount >= 1)

        // Validasi nomor HP salah
        vm.onPhoneChange("12345")
        vm.onAddressChange("Jl. Ijen No. 12, Malang")
        vm.saveCustomerProfile()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(!vm.uiState.value.isSaved)

        // Simpan valid
        val pickedLoc = GeoPoint(-7.9666, 112.6326)
        vm.onNameChange("Rani Putri")
        vm.onPhoneChange("081299988877")
        vm.onLocationPicked("Jl. Ijen No. 12, Malang", pickedLoc)
        vm.saveCustomerProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isSaved)
        assertEquals("Rani Putri", sessionStore.currentUserValue?.fullName)
        assertEquals("Jl. Ijen No. 12, Malang", sessionStore.currentUserValue?.defaultAddress)
        assertEquals(pickedLoc, sessionStore.currentUserValue?.defaultLocation)
    }

    @Test
    fun estimateViewModel_marksExpiredWhenCreateBookingFailsWithLockExpired() = runTest {
        val fakeLock = id.bantuin.app.domain.model.LockInfo(
            lockId = "expired_lock_id",
            providerId = "p01",
            startMin = 540,
            endMin = 660,
            expiresAtMillis = System.currentTimeMillis() - 10_000L,
            dateIso = "2026-11-22"
        )
        // Buat FakeRepository khusus yang menolak lock kedaluwarsa meski tidak ada di map
        val repoWithExpiredCheck = object : id.bantuin.app.domain.repository.BookingRepository by repository {
            override suspend fun createBooking(draft: id.bantuin.app.domain.model.OrderDraft): id.bantuin.app.core.result.AppResult<String> {
                return id.bantuin.app.core.result.AppResult.Error("Waktu penahanan slot habis, pilih jadwal lagi")
            }
        }
        val vm = EstimateViewModel(repoWithExpiredCheck)
        vm.initialize(
            categoryId = 2,
            provider = FakeData.providers.first(),
            service = FakeData.services.first(),
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Ijen",
            scheduledDateIso = "2026-11-22",
            scheduledStartMin = 540,
            scheduledDurationMin = 90,
            lockInfo = fakeLock,
            distanceKm = 1.5
        )

        vm.confirmOrder {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isLockExpired)
    }
}
