package id.bantuin.app.viewmodel

import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.core.dispatcher.AppDispatchers
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.CompletionStatus
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.ConflictReason
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.customer.estimate.EstimateViewModel
import id.bantuin.app.ui.customer.home.CustomerHomeViewModel
import id.bantuin.app.ui.customer.notifications.CustomerNotificationsViewModel
import id.bantuin.app.ui.customer.notifications.NotificationType
import id.bantuin.app.ui.customer.orderdetail.OrderDetailViewModel
import id.bantuin.app.ui.customer.profile.CustomerProfileViewModel
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
        repository = FakeBantuinRepository(calculationDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun customerHomeViewModel_loadsCategoriesAndUpdatesLocation() = runTest {
        val vm = CustomerHomeViewModel(repository, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.categories.isNotEmpty())
        assertEquals("Rani Wijaya", vm.uiState.value.user?.fullName)

        val newPoint = GeoPoint(-7.9467, 112.6156)
        vm.updateLocation("Jl. Suhat", newPoint)

        assertEquals("Jl. Suhat", vm.uiState.value.customerAddress)
        assertEquals(newPoint, vm.uiState.value.customerLocation)
    }

    @Test
    fun customerHomeViewModel_switchToProvider_withoutProviderRole_signsOutAndRedirects() = runTest {
        val sessionStore = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_customer_only",
                fullName = "Dimas Arya",
                email = "pelanggan2@bantuin.test",
                phone = "081234567891",
                roles = setOf("customer")
            )
        )
        val customerOnlyRepository = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = CustomerHomeViewModel(customerOnlyRepository, customerOnlyRepository)

        var allowed = false
        var unauthorized = false
        vm.switchToProvider(
            onAllowed = { allowed = true },
            onUnauthorized = { unauthorized = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!allowed)
        assertTrue(unauthorized)
        assertEquals(null, sessionStore.currentUserValue)
        assertEquals("customer", sessionStore.activeRoleValue)
    }

    @Test
    fun customerProfileViewModel_switchToProvider_withoutProviderRole_signsOutAndRedirects() = runTest {
        val sessionStore = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_customer_only",
                fullName = "Dimas Arya",
                email = "pelanggan2@bantuin.test",
                phone = "081234567891",
                roles = setOf("customer")
            )
        )
        val customerOnlyRepository = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = CustomerProfileViewModel(customerOnlyRepository)

        var allowed = false
        var unauthorized = false
        vm.switchRole(
            newRole = "provider",
            onAllowed = { allowed = true },
            onUnauthorized = { unauthorized = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!allowed)
        assertTrue(unauthorized)
        assertEquals(null, sessionStore.currentUserValue)
    }

    @Test
    fun customerProfileViewModel_switchToProvider_withProviderRole_switchesSuccessfully() = runTest {
        val sessionStore = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_multi_role",
                fullName = "Rani Wijaya",
                email = "pelanggan1@bantuin.test",
                phone = "081234567890",
                roles = setOf("customer", "provider")
            )
        )
        val repository = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = CustomerProfileViewModel(repository)

        var allowed = false
        var unauthorized = false
        vm.switchRole(
            newRole = "provider",
            onAllowed = { allowed = true },
            onUnauthorized = { unauthorized = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(allowed)
        assertTrue(!unauthorized)
        assertEquals("provider", sessionStore.activeRoleValue)
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

        // Gunakan besok agar validasi PAST_TIME tidak bergantung jam eksekusi test.
        vm.selectDate(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.selectedDateIdx)
        assertEquals(null, vm.uiState.value.nowMin)

        // Pilih slot jam yang bentrok -> alasan + alternatif harus tersedia
        vm.selectSlot(600) // 10:00 (bentrok dengan busyBlocks demo)
        assertEquals(ConflictReason.OVERLAP, vm.uiState.value.slotCheckResult?.reason)
        assertEquals(600, vm.uiState.value.selectedStartMin)
        assertEquals(listOf(825, 840, 870), vm.uiState.value.slotCheckResult?.alternativeStartsMin)

        // Slot alternatif pertama sudah lolos seluruh validasi F3 sehingga boleh dikunci
        vm.selectSlot(825)
        assertTrue(vm.uiState.value.slotCheckResult?.valid == true)

        var lockAcquired = false
        vm.acquireLockAndProceed { lockInfo ->
            lockAcquired = true
            assertNotNull(lockInfo.lockId)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(lockAcquired)
        assertNotNull(vm.uiState.value.lockInfo)
    }

    @Test
    fun scheduleViewModel_rejectsLockForInvalidSlot() = runTest {
        val vm = ScheduleViewModel(repository, repository)
        val provider = FakeData.providers.first { it.providerId == "p01" }
        val service = FakeData.services.first { it.providerId == "p01" }

        vm.initialize(provider, service, 1.8, 600)
        testDispatcher.scheduler.advanceUntilIdle()

        var lockAcquired = false
        vm.acquireLockAndProceed { lockAcquired = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!lockAcquired)
        assertEquals(null, vm.uiState.value.lockInfo)
    }

    @Test
    fun scheduleViewModel_dateSelectionUpdatesState() = runTest {
        val vm = ScheduleViewModel(repository, repository)
        val provider = FakeData.providers.first { it.providerId == "p01" }
        val service = FakeData.services.first { it.providerId == "p01" }

        vm.initialize(provider, service, 1.8, 600)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectDate(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.selectedDateIdx)
        assertEquals(null, vm.uiState.value.nowMin)
    }

    @Test
    fun orderDetailViewModel_missingBooking_isNotFoundInsteadOfInProgress() = runTest {
        val vm = OrderDetailViewModel(repository, repository)

        vm.observeBooking("booking_tidak_ada")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, vm.uiState.value.booking)
        assertTrue(vm.uiState.value.bookingNotFound)
        assertTrue(!vm.uiState.value.isLoadingBooking)
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
        val vm = OrderDetailViewModel(repository, repository, AppDispatchers(io = testDispatcher))
        vm.observeBooking("b01")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setRating(5)
        vm.setReviewComment("Sangat memuaskan")
        vm.toggleTag("Tepat Waktu")

        vm.setCompletion(CompletionStatus.TERLAMBAT)
        vm.submitReview()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sangat memuaskan", vm.uiState.value.booking?.review?.comment)
        assertEquals(CompletionStatus.TERLAMBAT, vm.uiState.value.booking?.outcome?.status)
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
}
