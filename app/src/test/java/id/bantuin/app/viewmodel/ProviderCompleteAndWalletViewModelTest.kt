package id.bantuin.app.viewmodel

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.provider.dashboard.ProviderDashboardViewModel
import id.bantuin.app.ui.provider.home.ProviderHomeViewModel
import id.bantuin.app.ui.provider.orders.CompleteOrderConfirmViewModel
import id.bantuin.app.ui.provider.wallet.ProviderWithdrawViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderCompleteAndWalletViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMitraRepo(id: String = "p01", name: String = "Budi Santoso, S.T."): FakeBantuinRepository {
        val store = InMemorySessionStore(
            initialRole = "provider",
            initialUser = UserProfile(
                id = id,
                fullName = name,
                email = "mitra@bantuin.test",
                phone = "081200000001",
                roles = setOf("customer", "provider")
            )
        )
        return FakeBantuinRepository(sessionStore = store)
    }

    @Test
    fun providerHomeViewModel_togglesAcceptingOrders_computesBalance_andExposesPriorityOrder() = runTest {
        val repo = createMitraRepo("p01", "Budi Santoso, S.T.")
        val vm = ProviderHomeViewModel(repo, repo, repo, repo, repo, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isAcceptingOrders)
        assertEquals(120_000, vm.uiState.value.availableBalance)
        assertEquals("b01", vm.uiState.value.priorityOrder?.id)
        assertEquals(BookingStatus.DALAM_PENGERJAAN, vm.uiState.value.priorityOrder?.status)

        vm.setAcceptingOrders(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isAcceptingOrders)

        vm.toggleAcceptingOrders()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isAcceptingOrders)
    }

    @Test
    fun completeOrderConfirmViewModel_blocksCashWithoutConfirmation_andCompletesWhenConfirmed() = runTest {
        val customerStore = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "c_cash_test",
                fullName = "Pelanggan Tunai",
                email = "cust@bantuin.test",
                phone = "081234567899",
                roles = setOf("customer")
            )
        )
        val repo = FakeBantuinRepository(sessionStore = customerStore)
        val provider = FakeData.providers.first { it.providerId == "p02" }
        val service = FakeData.services.first { it.providerId == "p02" }
        val created = repo.createBooking(
            OrderDraft(
                categoryId = service.categoryId,
                provider = provider,
                service = service,
                dateIso = "2026-11-26",
                startMin = 540,
                durationMin = service.durationMin,
                customerLocation = GeoPoint(-7.9797, 112.6304),
                customerAddress = "Jl. Ijen No. 10",
                distanceKm = 2.0,
                paymentMethod = PaymentMethod.CASH
            )
        ) as AppResult.Success
        val bookingId = created.data
        repo.updateStatus(bookingId, BookingStatus.DIKONFIRMASI)
        repo.updateStatus(bookingId, BookingStatus.DALAM_PENGERJAAN)

        val vm = CompleteOrderConfirmViewModel(repo)
        vm.loadBooking(bookingId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.canConfirm)

        // Coba selesai tanpa centang konfirmasi tunai
        vm.confirmCompletion()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isCompleted)
        assertNotNull(vm.uiState.value.errorMessage)

        // Centang konfirmasi tunai -> bisa selesai
        vm.setCashCollectedConfirmed(true)
        vm.onCompletionNoteChange("Selesai dan sudah dibayar tunai.")
        assertTrue(vm.uiState.value.canConfirm)

        vm.confirmCompletion()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isCompleted)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun completeOrderConfirmViewModel_completesPaidQrisBookingDirectly() = runTest {
        val repo = createMitraRepo("p01", "Budi Santoso, S.T.")
        val vm = CompleteOrderConfirmViewModel(repo)
        vm.loadBooking("b01")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.canConfirm)

        var callbackCalled = false
        vm.confirmCompletion { callbackCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isCompleted)
        assertTrue(callbackCalled)
    }

    @Test
    fun providerDashboardViewModel_loadsRevenueAnalyticsWithQrisAndCashBreakdown() = runTest {
        val repo = createMitraRepo("p01", "Budi Santoso, S.T.")
        val vm = ProviderDashboardViewModel(
            metricsRepository = repo,
            authRepository = repo,
            catalogRepository = repo,
            walletRepository = repo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val analytics = vm.uiState.value.revenueAnalytics
        assertEquals(185_000, analytics.totalCombinedRevenue)
        assertEquals(120_000, analytics.qrisGrossRevenue)
        assertEquals(65_000, analytics.cashRevenue)
        assertEquals(120_000, analytics.availableBalance)
        assertNotNull(vm.uiState.value.trustResult)
    }

    @Test
    fun providerWithdrawViewModel_validatesMinAmountBalanceAndAccount_thenRequestsPayout() = runTest {
        val repo = createMitraRepo("p01", "Budi Santoso, S.T.")
        val vm = ProviderWithdrawViewModel(repo, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(120_000, vm.uiState.value.availableBalance)

        // Nominal di bawah Rp20.000 ditolak
        vm.selectChannel(PayoutChannelType.BANK, "BCA")
        vm.onAccountNumberChange("1234567890")
        vm.onAccountHolderChange("Budi Santoso")
        vm.onAmountInputChange("15000")
        vm.submitPayout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.payouts.isEmpty())

        // Nominal melebihi saldo ditolak
        vm.onAmountInputChange("250000")
        vm.submitPayout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.payouts.isEmpty())

        // Data rekening kosong ditolak
        vm.onAccountNumberChange("")
        vm.onAmountInputChange("50000")
        vm.submitPayout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        // Penarikan valid Rp50.000 berhasil -> saldo berkurang dan riwayat bertambah
        vm.onAccountNumberChange("1234567890")
        vm.applyPresetAmount(50_000)
        assertEquals(50_000 - BantuinConfig.PAYOUT_ADMIN_FEE, vm.uiState.value.estimatedNetAmount)
        vm.submitPayout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.errorMessage)
        assertNotNull(vm.uiState.value.successMessage)
        assertEquals("", vm.uiState.value.amountInput)
        assertEquals(70_000, vm.uiState.value.availableBalance)
        assertEquals(1, vm.uiState.value.payouts.size)
        assertEquals(50_000, vm.uiState.value.payouts.first().amount)

        // Riwayat transaksi saldo harus memuat CREDIT (QRIS masuk) dan DEBIT (penarikan)
        assertTrue(vm.uiState.value.walletTransactions.any {
            it.type == id.bantuin.app.domain.model.WalletTransactionType.CREDIT && it.amount == 120_000
        })
        assertTrue(vm.uiState.value.walletTransactions.any {
            it.type == id.bantuin.app.domain.model.WalletTransactionType.DEBIT && it.amount == 50_000
        })

        vm.setHistoryFilter(id.bantuin.app.ui.provider.wallet.WalletHistoryFilter.CREDIT)
        assertTrue(vm.uiState.value.filteredTransactions.all {
            it.type == id.bantuin.app.domain.model.WalletTransactionType.CREDIT
        })

        vm.setHistoryFilter(id.bantuin.app.ui.provider.wallet.WalletHistoryFilter.DEBIT)
        assertTrue(vm.uiState.value.filteredTransactions.all {
            it.type == id.bantuin.app.domain.model.WalletTransactionType.DEBIT
        })
    }
}
