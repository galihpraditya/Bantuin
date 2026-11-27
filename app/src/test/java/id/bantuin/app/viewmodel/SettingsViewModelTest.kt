package id.bantuin.app.viewmodel

import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.core.settings.InMemorySettingsStore
import id.bantuin.app.core.settings.NotificationSettings
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun switchRoleWithoutSignOut_customerOnlyAccount_promptsProviderRegistrationWithoutLogout() = runTest {
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
        val repo = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = SettingsViewModel(repo, InMemoryThemeStore(), InMemorySettingsStore())
        testDispatcher.scheduler.advanceUntilIdle()

        var switched = false
        var needsRegistration = false
        vm.switchRoleWithoutSignOut(
            targetRole = "provider",
            onSwitched = { switched = true },
            onNeedsProviderRegistration = { needsRegistration = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(switched)
        assertTrue(needsRegistration)
        assertTrue(vm.uiState.value.needsProviderRegistration)
        assertNotNull("Akun tidak boleh keluar sesi saat butuh pendaftaran mitra", sessionStore.currentUserValue)
        assertEquals("customer", sessionStore.activeRoleValue)
    }

    @Test
    fun switchRoleWithoutSignOut_dualRoleAccount_switchesImmediatelyWithoutLogout() = runTest {
        val sessionStore = InMemorySessionStore(
            initialRole = "customer",
            initialUser = UserProfile(
                id = "user_dual",
                fullName = "Rani Mitra",
                email = "dual@bantuin.test",
                phone = "081234567890",
                roles = setOf("customer", "provider")
            )
        )
        val repo = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = SettingsViewModel(repo, InMemoryThemeStore(), InMemorySettingsStore())
        testDispatcher.scheduler.advanceUntilIdle()

        var switchedToProvider = false
        var needsReg = false
        vm.switchRoleWithoutSignOut(
            targetRole = "provider",
            onSwitched = { switchedToProvider = true },
            onNeedsProviderRegistration = { needsReg = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(switchedToProvider)
        assertFalse(needsReg)
        assertEquals("provider", sessionStore.activeRoleValue)
        assertNotNull(sessionStore.currentUserValue)

        var switchedToCustomer = false
        vm.switchRoleWithoutSignOut(
            targetRole = "customer",
            onSwitched = { switchedToCustomer = true },
            onNeedsProviderRegistration = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(switchedToCustomer)
        assertEquals("customer", sessionStore.activeRoleValue)
        assertNotNull(sessionStore.currentUserValue)
    }

    @Test
    fun activateProviderRole_validatesInputAndSavesCustomLocation() = runTest {
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
        val repo = FakeBantuinRepository(sessionStore = sessionStore)
        val vm = SettingsViewModel(repo, InMemoryThemeStore(), InMemorySettingsStore())
        testDispatcher.scheduler.advanceUntilIdle()

        vm.activateProviderRole(
            address = "",
            location = GeoPoint(-7.95, 112.61),
            bio = "",
            onSuccess = {}
        )
        assertNotNull(vm.uiState.value.errorMessage)

        var activated = false
        val customLoc = GeoPoint(-7.9467, 112.6156)
        vm.activateProviderRole(
            address = "Jl. Soekarno Hatta No. 12, Malang",
            location = customLoc,
            bio = "Teknisi listrik & AC",
            onSuccess = { activated = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(activated)
        assertTrue(sessionStore.currentUserValue?.roles?.contains("provider") == true)
        assertEquals("provider", sessionStore.activeRoleValue)
        val provider = (repo.providerProfile("user_customer_only") as id.bantuin.app.core.result.AppResult.Success).data
        assertEquals(customLoc, provider?.location)
    }

    @Test
    fun setThemeModeAndNotificationSettings_persistsInStores() = runTest {
        val repo = FakeBantuinRepository()
        val themeStore = InMemoryThemeStore()
        val settingsStore = InMemorySettingsStore()
        val vm = SettingsViewModel(repo, themeStore, settingsStore)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setThemeMode(ThemeMode.DARK)
        vm.updateNotificationSettings(
            NotificationSettings(
                orderUpdatesEnabled = false,
                scheduleReminderEnabled = true,
                providerQueueAlertEnabled = false,
                payoutAlertEnabled = true
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
        assertEquals(ThemeMode.DARK, themeStore.themeMode.first())
        assertFalse(vm.uiState.value.notificationSettings.orderUpdatesEnabled)
        assertFalse(settingsStore.settings.first().providerQueueAlertEnabled)
    }
}
