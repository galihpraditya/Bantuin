package id.bantuin.app.viewmodel

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.CompletionOutcome
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.SurgeReason
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.customer.estimate.EstimateViewModel
import id.bantuin.app.ui.customer.home.CustomerHomeViewModel
import id.bantuin.app.ui.customer.notifications.CustomerNotificationsViewModel
import id.bantuin.app.ui.customer.notifications.NotificationType
import id.bantuin.app.ui.customer.orderdetail.OrderDetailViewModel
import id.bantuin.app.ui.customer.orders.CustomerOrdersViewModel
import id.bantuin.app.ui.customer.profile.CustomerProfileViewModel
import id.bantuin.app.ui.customer.providerdetail.ProviderDetailViewModel
import id.bantuin.app.ui.customer.recommendation.ALL_CATEGORIES_ID
import id.bantuin.app.ui.customer.recommendation.ProviderSortOption
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

        // Uji reset & sorting berdasarkan jarak terdekat dan tarif terendah
        vm.resetFilters()
        assertEquals(0, vm.uiState.value.activeFilterCount)
        assertEquals(ProviderSortOption.RECOMMENDED, vm.uiState.value.sortOption)

        vm.setSortOption(ProviderSortOption.NEAREST)
        val distances = vm.uiState.value.filteredList.map { it.distanceKm }
        assertEquals(distances.sorted(), distances)

        vm.setSortOption(ProviderSortOption.LOWEST_PRICE)
        val prices = vm.uiState.value.filteredList.map { it.candidate.minBaseFare ?: Int.MAX_VALUE }
        assertEquals(prices.sorted(), prices)
    }

    @Test
    fun estimateViewModel_confirmOrderWithIncompleteStateShowsError() = runTest {
        val vm = EstimateViewModel(repository)
        vm.confirmOrder {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Data order tidak lengkap. Kembali dan pilih layanan serta jadwal lagi.",
            vm.uiState.value.errorMessage
        )
    }

    @Test
    fun recommendationViewModel_allCategoriesMergesCandidatesWithoutDuplicates() = runTest {
        val vm = RecommendationViewModel(repository)
        vm.initialize(categoryId = ALL_CATEGORIES_ID, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Semua Layanan", vm.uiState.value.category.name)
        assertTrue(vm.uiState.value.rawCandidates.isNotEmpty())
        val ids = vm.uiState.value.rawCandidates.map { it.providerId }
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun recommendationViewModel_radiusFeedsBackIntoMatchmaking() = runTest {
        val vm = RecommendationViewModel(repository)
        vm.initialize(categoryId = ALL_CATEGORIES_ID, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()
        val defaultOutOfRange = vm.uiState.value.matchResult.outOfRange.size

        vm.setMaxDistanceFilter(3.0)
        assertTrue(vm.uiState.value.filteredList.all { it.distanceKm <= 3.0 })
        assertTrue(vm.uiState.value.matchResult.outOfRange.size > defaultOutOfRange)
    }

    @Test
    fun estimateViewModel_urgentToggleChangesTariffAndPersistsToDraft() = runTest {
        var capturedDraft: OrderDraft? = null
        val spyRepository = object : BookingRepository by repository {
            override suspend fun createBooking(draft: OrderDraft): AppResult<String> {
                capturedDraft = draft
                return AppResult.Success("bti_urgent_test")
            }
        }
        val vm = EstimateViewModel(spyRepository)
        vm.initialize(
            categoryId = 2,
            provider = FakeData.providers.first(),
            service = FakeData.services.first(),
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Alun-alun",
            scheduledDateIso = "2026-11-26",
            scheduledStartMin = 600, // 10:00, bukan jam malam
            scheduledDurationMin = 90,
            distanceKm = 2.0
        )
        val normalTotal = vm.uiState.value.costEstimate?.total ?: 0

        vm.setUrgent(true)
        val urgentTotal = vm.uiState.value.costEstimate?.total ?: 0
        assertTrue(urgentTotal > normalTotal)
        assertEquals(SurgeReason.URGENT, vm.uiState.value.costEstimate?.surgeReason)

        vm.confirmOrder {}
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, capturedDraft?.isUrgent)
    }

    @Test
    fun scheduleViewModel_validatesSlotBeforeProceeding() = runTest {
        // Jam 08:00 WIB agar slot 10:00 masih tampil (dan bentrok dengan blok demo)
        val vm = ScheduleViewModel(
            repository,
            repository,
            nowProvider = { java.time.LocalTime.of(8, 0) }
        )
        val provider = FakeData.providers.first { it.providerId == "p01" }
        val service = FakeData.services.first { it.providerId == "p01" }

        vm.initialize(provider = provider, service = service, distanceKm = 1.8)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(7, vm.uiState.value.dynamicDates.size)

        // Slot bentrok tidak boleh melanjutkan ke estimasi
        vm.selectSlot(600) // 10:00 (bentrok dengan busyBlocks demo)
        assertTrue(vm.uiState.value.slotCheckResult != null)
        assertEquals(false, vm.uiState.value.slotCheckResult?.valid)

        var proceeded = false
        vm.validateAndProceed { proceeded = true }
        assertTrue(!proceeded)
        assertNotNull(vm.uiState.value.errorMessage)

        // Slot valid (besok 14:00) boleh melanjutkan tanpa mengunci slot
        vm.selectDate(1)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.selectSlot(840)
        vm.validateAndProceed { proceeded = true }
        assertTrue(proceeded)
        assertEquals(null, vm.uiState.value.errorMessage)
    }

    @Test
    fun scheduleViewModel_hidesPastSlotsOnToday() = runTest {
        // Jam 14:30 WIB -> slot 14:00 dan sebelumarnya tidak boleh ditampilkan
        val vm = ScheduleViewModel(
            repository,
            repository,
            nowProvider = { java.time.LocalTime.of(14, 30) }
        )
        val provider = FakeData.providers.first { it.providerId == "p09" } // ALWAYS_ON
        val service = FakeData.services.first().copy(providerId = "p09", durationMin = 30)

        vm.initialize(provider = provider, service = service, distanceKm = 1.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val slotMins = vm.uiState.value.availableSlots.map { it.first }
        assertTrue(slotMins.isNotEmpty())
        assertTrue(slotMins.min() > 870)
        assertTrue(!slotMins.contains(840))
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
    fun orderDetailViewModel_savesReopensEditsAndDeletesReview() = runTest {
        repository.signIn("pelanggan2@bantuin.test", "Demo12345!")
        val vm = OrderDetailViewModel(repository, repository)
        vm.observeBooking("b00_qris")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setCompletion(CompletionOutcome.TERLAMBAT)
        vm.setRating(1)
        vm.setReviewComment("Perlu perbaikan")
        vm.submitReview()
        testDispatcher.scheduler.advanceUntilIdle()

        val reopened = OrderDetailViewModel(repository, repository)
        reopened.observeBooking("b00_qris")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(reopened.uiState.value.reviewSubmitted)
        assertEquals(1, reopened.uiState.value.rating)
        assertEquals("Perlu perbaikan", reopened.uiState.value.reviewComment)
        assertEquals(CompletionOutcome.TERLAMBAT, reopened.uiState.value.completion)

        reopened.setCompletion(CompletionOutcome.SELESAI_TEPAT_WAKTU)
        reopened.setRating(5)
        reopened.submitReview()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, reopened.uiState.value.booking?.review?.rating)

        reopened.deleteReview()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(!reopened.uiState.value.reviewSubmitted)
        assertEquals(null, reopened.uiState.value.booking?.review)
        assertEquals(CompletionOutcome.SELESAI_TEPAT_WAKTU, reopened.uiState.value.completion)
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
        repository.signIn("pelanggan2@bantuin.test", "Demo12345!")
        vm.observeBooking("b00_qris")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setCompletion(CompletionOutcome.SELESAI_TEPAT_WAKTU)
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
    fun estimateViewModel_surfacesCreateBookingErrorWithoutLock() = runTest {
        val repoWithError = object : id.bantuin.app.domain.repository.BookingRepository by repository {
            override suspend fun createBooking(draft: id.bantuin.app.domain.model.OrderDraft): id.bantuin.app.core.result.AppResult<String> {
                return id.bantuin.app.core.result.AppResult.Error("Gagal membuat pesanan")
            }
        }
        val vm = EstimateViewModel(repoWithError)
        vm.initialize(
            categoryId = 2,
            provider = FakeData.providers.first(),
            service = FakeData.services.first(),
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Ijen",
            scheduledDateIso = "2026-11-22",
            scheduledStartMin = 540,
            scheduledDurationMin = 90,
            distanceKm = 1.5
        )

        vm.confirmOrder {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Gagal membuat pesanan", vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isSubmitting)
    }
}
