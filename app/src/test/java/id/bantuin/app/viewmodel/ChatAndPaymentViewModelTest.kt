package id.bantuin.app.viewmodel

import id.bantuin.app.core.chat.InMemoryChatReadStore
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.ChatMessage
import id.bantuin.app.domain.model.ChatThreadSummary
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.ChatRepository
import id.bantuin.app.ui.chat.ChatListViewModel
import id.bantuin.app.ui.chat.ChatRoomViewModel
import id.bantuin.app.ui.customer.estimate.EstimateViewModel
import id.bantuin.app.ui.payment.QrisPaymentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
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
class ChatAndPaymentViewModelTest {

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
    fun estimateViewModel_defaultsToCash_switchesToQris_andCreatesQrisBooking() = runTest {
        val vm = EstimateViewModel(repository)
        val provider = FakeData.providers.first()
        val service = FakeData.services.first()

        vm.initialize(
            categoryId = 2,
            provider = provider,
            service = service,
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Ijen No. 25, Malang",
            scheduledDateIso = "2026-11-26",
            scheduledStartMin = 600,
            scheduledDurationMin = 90,
            distanceKm = 2.0
        )

        assertEquals(PaymentMethod.CASH, vm.uiState.value.paymentMethod)

        vm.setPaymentMethod(PaymentMethod.QRIS)
        assertEquals(PaymentMethod.QRIS, vm.uiState.value.paymentMethod)

        var createdId: String? = null
        vm.confirmOrder { createdId = it }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(createdId)
        val createdBooking = repository.observeBooking(createdId!!).first()
        assertNotNull(createdBooking)
        assertEquals(PaymentMethod.QRIS, createdBooking?.paymentMethod)
        assertEquals(PaymentStatus.WAITING_PAYMENT, createdBooking?.paymentStatus)
    }

    @Test
    fun qrisPaymentViewModel_confirmSimulatedPayment_marksBookingPaid() = runTest {
        val estimateVm = EstimateViewModel(repository)
        val provider = FakeData.providers.first()
        val service = FakeData.services.first()

        estimateVm.initialize(
            categoryId = 2,
            provider = provider,
            service = service,
            customerLocation = FakeData.MALANG_CENTER,
            customerAddress = "Jl. Veteran, Malang",
            scheduledDateIso = "2026-11-26",
            scheduledStartMin = 660,
            scheduledDurationMin = 90,
            distanceKm = 1.5
        )
        estimateVm.setPaymentMethod(PaymentMethod.QRIS)

        var bookingId = ""
        estimateVm.confirmOrder { bookingId = it }
        testDispatcher.scheduler.advanceUntilIdle()

        val qrisVm = QrisPaymentViewModel(repository)
        qrisVm.loadBooking(bookingId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(qrisVm.uiState.value.isPaid)
        assertEquals(bookingId, qrisVm.uiState.value.booking?.id)

        var callbackTriggered = false
        qrisVm.confirmSimulatedPayment { callbackTriggered = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackTriggered)
        assertTrue(qrisVm.uiState.value.isPaid)
        assertEquals(PaymentStatus.PAID, qrisVm.uiState.value.booking?.paymentStatus)
    }

    @Test
    fun chatRoomViewModel_rejectsBlankInput_sendsMessageAndClearsDraft() = runTest {
        val vm = ChatRoomViewModel(repository, repository)
        vm.initialize("b01")
        testDispatcher.scheduler.advanceUntilIdle()

        val initialMessages = vm.uiState.value.messages
        assertTrue(initialMessages.isNotEmpty())
        assertTrue(initialMessages.zipWithNext().all { (a, b) -> a.createdAt <= b.createdAt })

        vm.onMessageInputChange("   ")
        vm.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(initialMessages.size, vm.uiState.value.messages.size)

        vm.onMessageInputChange("Mohon bawa tangga lipat ya Pak")
        vm.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", vm.uiState.value.messageInput)
        assertTrue(vm.uiState.value.messages.size > initialMessages.size)
        assertTrue(vm.uiState.value.messages.any { it.text == "Mohon bawa tangga lipat ya Pak" })
        val sorted = vm.uiState.value.messages.sortedBy { it.createdAt }
        assertEquals(sorted, vm.uiState.value.messages)
    }

    @Test
    fun chatListViewModel_exposesFilteredThreadsAndThreeUiStates() = runTest {
        val vm = ChatListViewModel(repository, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialState = vm.uiState.value
        assertFalse(initialState.isLoading)
        assertEquals(null, initialState.errorMessage)
        assertTrue(initialState.threads.isNotEmpty())

        val firstCounterparty = initialState.threads.first().counterpartyName
        vm.onSearchQueryChange(firstCounterparty.take(4))
        assertTrue(vm.uiState.value.filteredThreads.isNotEmpty())

        vm.onSearchQueryChange("zzzz_tidak_ada_sama_sekali")
        assertTrue(vm.uiState.value.filteredThreads.isEmpty())
    }

    @Test
    fun chatListViewModel_keepsErrorWhenThreadStreamFails() = runTest {
        val failingRepository = object : ChatRepository {
            override fun observeChatThreads(role: String): Flow<List<ChatThreadSummary>> =
                flow { throw IllegalStateException("Gagal RLS booking_messages") }
            override fun observeMessages(bookingId: String): Flow<List<ChatMessage>> = flowOf(emptyList())
            override suspend fun sendMessage(bookingId: String, text: String): AppResult<Unit> =
                AppResult.Success(Unit)
        }
        val vm = ChatListViewModel(failingRepository, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.threads.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun chatRoomViewModel_retryMessagesSurfacesLoadError() = runTest {
        val failingRepository = object : ChatRepository by repository {
            override fun observeChatError(bookingId: String): Flow<String?> =
                flowOf("Gagal memuat percakapan")
            override suspend fun retryMessages(bookingId: String): AppResult<Unit> =
                AppResult.Error("Masih gagal memuat percakapan")
        }
        val vm = ChatRoomViewModel(failingRepository, repository)
        vm.initialize("b01")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Gagal memuat percakapan", vm.uiState.value.errorMessage)

        vm.retryMessages()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Masih gagal memuat percakapan", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun chatUnread_disappearsAfterMarkingThreadRead() = runTest {
        val readStore = InMemoryChatReadStore()
        val repoWithReadStore = FakeBantuinRepository(chatReadStore = readStore)

        val before = repoWithReadStore.observeChatThreads("customer").first()
        val threadBefore = before.first { it.bookingId == "b01" }
        assertTrue(threadBefore.unreadCount > 0)

        repoWithReadStore.markThreadRead("b01")
        val after = repoWithReadStore.observeChatThreads("customer").first()
        assertEquals(0, after.first { it.bookingId == "b01" }.unreadCount)

        // Mundurkan marker: pesan lama jadi "belum dibaca" lagi
        readStore.markRead("user_demo_rani", "b01", Instant.fromEpochMilliseconds(0))
        val newer = repoWithReadStore.observeChatThreads("customer").first()
        assertTrue(newer.first { it.bookingId == "b01" }.unreadCount > 0)
    }
}
