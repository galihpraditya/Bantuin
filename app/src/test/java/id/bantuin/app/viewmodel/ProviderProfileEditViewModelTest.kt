package id.bantuin.app.viewmodel

import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.provider.profile.ProviderProfileEditViewModel
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
class ProviderProfileEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var fakeRepository: FakeBantuinRepository
    private lateinit var viewModel: ProviderProfileEditViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionStore = InMemorySessionStore(
            initialRole = "provider",
            initialUser = UserProfile(
                id = "p01",
                fullName = "Budi Santoso, S.T.",
                email = "mitra01@bantuin.test",
                phone = "081234567801",
                roles = setOf("provider", "customer")
            )
        )
        fakeRepository = FakeBantuinRepository(sessionStore = sessionStore)
        viewModel = ProviderProfileEditViewModel(fakeRepository, fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadProfile_loadsFromLoggedInUserAndCatalog() = runTest {
        viewModel.loadProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("p01", state.providerId)
        assertEquals("Budi Santoso, S.T.", state.name)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isSaved)
    }

    @Test
    fun formUpdates_updateStateReactively() = runTest {
        viewModel.loadProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNameChange("Pak Budi Senior")
        viewModel.onBioChange("Spesialis Servis AC Inverter")
        viewModel.onPhoneChange("081299998888")
        viewModel.onAddressChange("Jl. Danau Toba No. 12, Sawojajar")
        val newLoc = GeoPoint(-7.9772, 112.6562)
        viewModel.onLocationChange(newLoc)

        val state = viewModel.uiState.value
        assertEquals("Pak Budi Senior", state.name)
        assertEquals("Spesialis Servis AC Inverter", state.bio)
        assertEquals("081299998888", state.phone)
        assertEquals("Jl. Danau Toba No. 12, Sawojajar", state.address)
        assertEquals(newLoc, state.location)
    }

    @Test
    fun validation_failsWhenNameIsBlank() = runTest {
        viewModel.loadProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNameChange("   ")
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
        assertEquals("Nama lengkap tidak boleh kosong", state.errorMessage)
    }

    @Test
    fun validation_failsWhenPhoneFormatIsInvalid() = runTest {
        viewModel.loadProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPhoneChange("abc123xyz")
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
        assertEquals("Format nomor telepon tidak valid", state.errorMessage)
    }

    @Test
    fun saveProfile_savesSuccessfully() = runTest {
        viewModel.loadProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNameChange("Budi Santoso AC Pro")
        viewModel.onBioChange("Servis AC Cepat & Bergaransi")
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertTrue(state.isSaved)
        assertNull(state.errorMessage)

        // Reset saved
        viewModel.resetSaved()
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
