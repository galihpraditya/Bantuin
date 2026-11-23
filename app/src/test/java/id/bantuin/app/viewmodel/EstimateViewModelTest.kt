package id.bantuin.app.viewmodel

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.LockInfo
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.SurgeReason
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.ui.customer.estimate.EstimateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EstimateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeBookingRepository : BookingRepository {
        var createBookingCallCount = 0
        var shouldFail = false
        var failureMessage = "Gagal membuat pesanan"
        var lastDraft: OrderDraft? = null

        override suspend fun createBooking(draft: OrderDraft): AppResult<String> {
            createBookingCallCount++
            lastDraft = draft
            return if (shouldFail) {
                AppResult.Error(failureMessage)
            } else {
                AppResult.Success("booking_12345")
            }
        }

        override suspend fun busyBlocks(providerId: String, dateIso: String): AppResult<List<id.bantuin.app.domain.model.TimeBlock>> =
            AppResult.Success(emptyList())

        override suspend fun acquireLock(providerId: String, startMin: Int, endMin: Int): AppResult<LockInfo> =
            AppResult.Success(LockInfo("lock_1", "p01", startMin, endMin, System.currentTimeMillis() + 600_000))

        override suspend fun releaseLock(lockId: String): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun updateStatus(bookingId: String, newStatus: id.bantuin.app.domain.model.BookingStatus): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun myBookingsCustomer(): AppResult<List<id.bantuin.app.domain.model.BookingItem>> =
            AppResult.Success(emptyList())

        override suspend fun myBookingsProvider(): AppResult<List<id.bantuin.app.domain.model.BookingItem>> =
            AppResult.Success(emptyList())

        override fun observeBooking(bookingId: String): kotlinx.coroutines.flow.Flow<id.bantuin.app.domain.model.BookingItem?> =
            kotlinx.coroutines.flow.flowOf(null)

        override fun observeCustomerBookings(): kotlinx.coroutines.flow.Flow<List<id.bantuin.app.domain.model.BookingItem>> =
            kotlinx.coroutines.flow.flowOf(emptyList())
    }

    private lateinit var fakeRepo: FakeBookingRepository
    private lateinit var viewModel: EstimateViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeBookingRepository()
        viewModel = EstimateViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel(initialLock: LockInfo? = LockInfo("lock_123", "p01", 600, 690, System.currentTimeMillis() + 600_000)) {
        viewModel.initialize(
            categoryId = 2,
            provider = FakeData.providers.first(),
            service = FakeData.services.first(),
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Ijen No. 1",
            scheduledDateIso = "2026-11-23",
            scheduledStartMin = 600, // 10:00 (normal)
            scheduledDurationMin = 90,
            lockInfo = initialLock,
            distanceKm = 2.0,
            initialComplexity = Complexity.SEDANG,
            initialUrgent = false,
            initialNotes = "Initial note"
        )
    }

    @Test
    fun testInitialization_calculatesEstimateAndSetsInitialValues() {
        initViewModel()
        val state = viewModel.uiState.value

        assertEquals("Initial note", state.notes)
        assertEquals(Complexity.SEDANG, state.complexity)
        assertFalse(state.isUrgent)
        assertFalse(state.isLockExpired)
        assertNotNull(state.costEstimate)
        assertEquals(600L, state.remainingLockSeconds)
    }

    @Test
    fun testSetComplexity_recalculatesCostEstimateInstantly() {
        initViewModel()
        val initialTotal = viewModel.uiState.value.costEstimate!!.total

        viewModel.setComplexity(Complexity.BERAT)
        val state = viewModel.uiState.value

        assertEquals(Complexity.BERAT, state.complexity)
        assertTrue(state.costEstimate!!.total > initialTotal)
    }

    @Test
    fun testSetUrgent_appliesSurgeAndUpdatesCostEstimate() {
        initViewModel()
        val initialTotal = viewModel.uiState.value.costEstimate!!.total

        viewModel.setUrgent(true)
        val state = viewModel.uiState.value

        assertTrue(state.isUrgent)
        assertTrue(state.costEstimate!!.surgeApplied)
        assertEquals(SurgeReason.URGENT, state.costEstimate!!.surgeReason)
        assertTrue(state.costEstimate!!.total > initialTotal)
    }

    @Test
    fun testSetNotes_updatesNotesState() {
        initViewModel()
        viewModel.setNotes("Mohon bawa tangga panjang")
        assertEquals("Mohon bawa tangga panjang", viewModel.uiState.value.notes)
    }

    @Test
    fun testLockCountdown_expiresLockAndBlocksOrderSubmission() = runTest {
        initViewModel()
        assertFalse(viewModel.uiState.value.isLockExpired)

        // Hitung mundur menuju 0
        viewModel.updateLockCountdown(0L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLockExpired)
        assertEquals(0L, viewModel.uiState.value.remainingLockSeconds)

        // Percobaan kirim pesanan saat lock expired harus ditolak
        var successCalled = false
        viewModel.confirmOrder { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(0, fakeRepo.createBookingCallCount)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("kedaluwarsa", ignoreCase = true))
    }

    @Test
    fun testConfirmOrder_success() = runTest {
        initViewModel()
        var createdId: String? = null

        viewModel.confirmOrder { id -> createdId = id }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepo.createBookingCallCount)
        assertEquals("booking_12345", createdId)
        assertEquals("booking_12345", viewModel.uiState.value.createdBookingId)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun testConfirmOrder_failure_setsErrorMessage() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureMessage = "SLOT_CONFLICT: slot telah diambil pengguna lain"
        initViewModel()

        var successCalled = false
        viewModel.confirmOrder { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepo.createBookingCallCount)
        assertFalse(successCalled)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals("SLOT_CONFLICT: slot telah diambil pengguna lain", viewModel.uiState.value.errorMessage)
    }
}
