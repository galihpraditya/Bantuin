package id.bantuin.app.viewmodel

import id.bantuin.app.core.dispatcher.AppDispatchers
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.CompletionStatus
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.provider.calendar.ProviderCalendarViewModel
import id.bantuin.app.ui.provider.dashboard.ProviderDashboardViewModel
import id.bantuin.app.ui.provider.home.ProviderHomeViewModel
import id.bantuin.app.ui.provider.orders.ProviderOrdersViewModel
import id.bantuin.app.ui.provider.profile.ProviderProfileEditViewModel
import id.bantuin.app.ui.provider.services.ProviderServicesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class ProviderViewModelsTest {

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

    private fun mitraRepository(id: String, name: String): FakeBantuinRepository {
        val sessionStore = InMemorySessionStore(
            initialRole = "provider",
            initialUser = UserProfile(
                id = id,
                fullName = name,
                email = "mitra@bantuin.test",
                phone = "081200000000",
                roles = setOf("provider", "customer")
            )
        )
        return FakeBantuinRepository(sessionStore = sessionStore)
    }

    @Test
    fun providerHomeViewModel_loadsDataAndSwitchesToCustomer() = runTest {
        val vm = ProviderHomeViewModel(repository, repository, repository, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.provider)

        var switched = false
        vm.switchToCustomer { switched = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(switched)
        assertEquals("customer", repository.activeRole().first())
    }

    @Test
    fun providerHomeViewModel_usesLoggedInMitra_notBudisProfile() = runTest {
        val mitraRepository = mitraRepository("p02", "Ahmad Fauzi")
        val vm = ProviderHomeViewModel(mitraRepository, mitraRepository, mitraRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("p02", vm.uiState.value.provider?.providerId)
        assertEquals("Ahmad Fauzi", vm.uiState.value.user?.fullName)
    }

    @Test
    fun providerOrdersViewModel_updatesOrderStatus() = runTest {
        val vm = ProviderOrdersViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectTab(1)
        assertEquals(1, vm.uiState.value.selectedTab)

        vm.updateStatus("b01", BookingStatus.DALAM_PENGERJAAN)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!vm.uiState.value.isUpdatingStatus)
    }

    @Test
    fun providerCalendarViewModel_validatesOperatingHours() = runTest {
        val vm = ProviderCalendarViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.availabilities.isNotEmpty())

        // Ubah mode ke ALWAYS_ON
        vm.updateDayMode(1, AvailabilityMode.ALWAYS_ON)
        assertEquals(AvailabilityMode.ALWAYS_ON, vm.uiState.value.availabilities.first { it.dayOfWeek == 1 }.mode)

        // Validasi: jam buka >= jam tutup harus dicegah
        vm.updateOperatingHours(1, openMin = 1000, closeMin = 500)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun providerServicesViewModel_savesService() = runTest {
        val vm = ProviderServicesViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val newService = ServiceItem(
            id = "svc_test_01",
            providerId = "p01",
            categoryId = 2,
            name = "Pembersihan Saluran AC",
            description = "Pembersihan menyeluruh",
            baseFare = 75000,
            durationMin = 60,
            isActive = true
        )
        vm.saveService(newService)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.services.any { it.name == "Pembersihan Saluran AC" })
    }

    @Test
    fun providerProfileEditViewModel_updatesProfile() = runTest {
        val vm = ProviderProfileEditViewModel(repository)
        vm.loadProfile("p01")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onNameChange("Pak Budi Santoso Senior")
        vm.onBioChange("Teknisi AC Berpengalaman")
        vm.onLocationChange(GeoPoint(-7.9500, 112.6200))
        vm.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isSaved)
    }

    @Test
    fun providerDashboardViewModel_usesLoggedInMitraAccount() = runTest {
        val mitraRepository = mitraRepository("p02", "Ahmad Fauzi")
        val vm = ProviderDashboardViewModel(mitraRepository, mitraRepository, mitraRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("p02", vm.uiState.value.providerId)
        assertEquals("Ahmad Fauzi", vm.uiState.value.providerName)
        assertEquals("Ahmad Fauzi", vm.uiState.value.user?.fullName)
    }

    @Test
    fun providerDashboardViewModel_evaluatesTheSelectedProvidersHistory() = runTest {
        val vm = ProviderDashboardViewModel(repository, dispatchers = AppDispatchers(
            io = testDispatcher, default = testDispatcher
        ))
        vm.loadData("p01")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.trustResult)
        assertTrue(vm.uiState.value.trustResult?.flagAnomaly == false)

        vm.loadData("p05")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("p05", vm.uiState.value.providerId)
        assertTrue(vm.uiState.value.trustResult?.flagAnomaly == true)
        assertTrue(vm.uiState.value.transactions.any { it.status == CompletionStatus.DIBATALKAN })

        vm.filterHistory(CompletionStatus.DIBATALKAN)
        assertEquals(CompletionStatus.DIBATALKAN, vm.uiState.value.historyFilter)
        vm.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.snapshots.isNotEmpty())
    }
}
