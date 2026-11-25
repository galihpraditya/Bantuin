package id.bantuin.app.viewmodel

import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.provider.calendar.ProviderCalendarViewModel
import id.bantuin.app.ui.provider.dashboard.ProviderDashboardViewModel
import id.bantuin.app.ui.provider.evaluation.ProviderEvaluationViewModel
import id.bantuin.app.ui.provider.home.ProviderHomeViewModel
import id.bantuin.app.ui.provider.orders.ProviderOrdersViewModel
import id.bantuin.app.ui.provider.profile.ProviderProfileEditViewModel
import id.bantuin.app.ui.provider.services.ProviderServicesViewModel
import java.util.UUID
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
        repository = FakeBantuinRepository()
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
    fun providerHomeViewModel_usesLoggedInMitra_notBudisProfile() = runTest {
        val mitraRepository = mitraRepository("p02", "Ahmad Fauzi")
        val vm = ProviderHomeViewModel(mitraRepository, mitraRepository, mitraRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("p02", vm.uiState.value.provider?.providerId)
        assertEquals("Ahmad Fauzi", vm.uiState.value.user?.fullName)

        var switched = false
        var unauthorized = false
        vm.switchToCustomer(
            onAllowed = { switched = true },
            onUnauthorized = { unauthorized = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(switched)
        assertTrue(!unauthorized)
        assertEquals("customer", mitraRepository.activeRole().first())
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
        val vm = ProviderCalendarViewModel(repository, repository)
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
        val mitraRepository = mitraRepository("p01", "Budi Santoso, S.T.")
        val vm = ProviderServicesViewModel(mitraRepository, mitraRepository)
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
        val vm = ProviderProfileEditViewModel(repository, repository)
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
    fun providerDashboardViewModel_evaluatesTrustAndSimulatesAnomaly() = runTest {
        val mitraRepository = mitraRepository("p02", "Ahmad Fauzi")
        val vm = ProviderDashboardViewModel(mitraRepository, mitraRepository, mitraRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.trustResult)
        val initialTrust = vm.uiState.value.trustResult?.trustScore ?: 0.0
        assertTrue(initialTrust >= 60.0)

        // Simulasikan rating anjlok ke 1.0 dan cancel rate tinggi
        vm.updateSimulation(rating = 1.0, lateCount = 10, cancelCount = 15)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isSimulationActive)
        val simTrust = vm.uiState.value.simResult?.trustScore ?: 100.0
        assertTrue(simTrust < 60.0)
        assertTrue(vm.uiState.value.simResult?.flagAnomaly == true)
    }

    @Test
    fun providerHomeViewModel_computesLiveTrustScoreAndSpecialty() = runTest {
        val mitraRepo = mitraRepository("p01", "Budi Santoso, S.T.")
        val vm = ProviderHomeViewModel(mitraRepo, mitraRepo, mitraRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.trustResult)
        assertTrue(vm.uiState.value.specialtyLabel.isNotBlank())
    }

    @Test
    fun providerOrdersViewModel_runsExpirePendingAndObservesFlow() = runTest {
        val vm = ProviderOrdersViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.hasSyncedPendingExpiry)
    }

    @Test
    fun providerCalendarViewModel_loadsUpcomingBookings() = runTest {
        val mitraRepo = mitraRepository("p01", "Budi Santoso, S.T.")
        val vm = ProviderCalendarViewModel(mitraRepo, mitraRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.upcomingBookingsLoaded)
    }

    @Test
    fun providerServicesViewModel_createsUuidServiceAndAllowsZeroFare() = runTest {
        val mitraRepo = mitraRepository("p01", "Budi Santoso, S.T.")
        val vm = ProviderServicesViewModel(mitraRepo, mitraRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.saveServiceFromForm(
            existing = null,
            name = "Survei Lokasi Awal",
            description = "Kunjungan pengecekan awal",
            categoryId = 2,
            baseFareText = "0",
            durationText = "30"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val created = vm.uiState.value.services.firstOrNull { it.name == "Survei Lokasi Awal" }
        assertNotNull(created)
        assertEquals(0, created?.baseFare)
        // Harus UUID valid untuk kolom PostgreSQL uuid
        UUID.fromString(created!!.id)
    }

    @Test
    fun providerProfileEditViewModel_validatesInvalidPhone() = runTest {
        val mitraRepo = mitraRepository("p01", "Budi Santoso, S.T.")
        val vm = ProviderProfileEditViewModel(mitraRepo, mitraRepo)
        vm.loadProfile("p01")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onPhoneChange("abc123")
        vm.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Nomor telepon Indonesia tidak valid (contoh: 081234567890)",
            vm.uiState.value.errorMessage
        )
    }

    @Test
    fun providerDashboardViewModel_updatesThemeModeAndRespondsToPendingOrder() = runTest {
        val mitraRepo = mitraRepository("p01", "Budi Santoso, S.T.")
        val themeStore = InMemoryThemeStore()
        val vm = ProviderDashboardViewModel(
            metricsRepository = mitraRepo,
            authRepository = mitraRepo,
            catalogRepository = mitraRepo,
            bookingRepository = mitraRepo,
            themeStore = themeStore
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
        vm.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    @Test
    fun providerEvaluationViewModel_loadsAndRecalculatesTrustMetrics() = runTest {
        val mitraRepo = mitraRepository("p01", "Budi Santoso, S.T.")
        val vm = ProviderEvaluationViewModel(mitraRepo, mitraRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.trustResult)
        vm.recalculate()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.recalculatedRecently)
        assertTrue(vm.uiState.value.lastSyncLabel.isNotBlank())
    }
}
