package id.bantuin.app.viewmodel

import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBantuinRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBantuinRepository()
        viewModel = AuthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signIn_withBlankCredentials_showsErrorMessage() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("")
        var successCalled = false

        viewModel.signIn { successCalled = true }

        assertTrue(!successCalled)
        assertEquals("Email dan password wajib diisi", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signIn_withValidCredentials_succeeds() = runTest {
        viewModel.onEmailChange("rani@bantuin.test")
        viewModel.onPasswordChange("Demo12345!")
        var returnedUser: UserProfile? = null

        viewModel.signIn { user -> returnedUser = user }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(returnedUser)
        assertEquals("user_demo_rani", returnedUser?.id)
        assertEquals("Rani Wijaya", returnedUser?.fullName)
        assertEquals("rani@bantuin.test", returnedUser?.email)
        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signIn_withWrongPassword_isRejected() = runTest {
        viewModel.onEmailChange("pelanggan1@bantuin.test")
        viewModel.onPasswordChange("kata-sandi-salah")
        var successCalled = false

        viewModel.signIn { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!successCalled)
        assertTrue(!viewModel.uiState.value.isSuccess)
        assertEquals("Email atau kata sandi salah.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signIn_asProvider_returnsProviderProfile() = runTest {
        viewModel.onEmailChange("mitra01@bantuin.test")
        viewModel.onPasswordChange("Demo12345!")
        var returnedUser: UserProfile? = null

        viewModel.signIn { user -> returnedUser = user }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("p01", returnedUser?.id)
        assertEquals("Budi Santoso, S.T.", returnedUser?.fullName)
        assertEquals(setOf("provider"), returnedUser?.roles)
    }

    @Test
    fun signUp_withValidData_succeeds() = runTest {
        viewModel.onNameChange("Dewi Sartika")
        viewModel.onEmailChange("dewi@bantuin.test")
        viewModel.onPasswordChange("Rahasia123!")
        viewModel.onPhoneChange("081298765432")
        viewModel.onRoleToggle(true)
        var registeredUser: UserProfile? = null

        viewModel.signUp { user -> registeredUser = user }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(registeredUser)
        assertEquals("Dewi Sartika", registeredUser?.fullName)
        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun signIn_withInvalidEmailFormat_showsValidationError() = runTest {
        viewModel.onEmailChange("bukan-email")
        viewModel.onPasswordChange("Demo12345!")
        var successCalled = false

        viewModel.signIn { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!successCalled)
        assertEquals("Format email tidak valid", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signUp_withShortPassword_showsValidationError() = runTest {
        viewModel.onNameChange("Dewi Sartika")
        viewModel.onEmailChange("dewi@bantuin.test")
        viewModel.onPasswordChange("12345")
        viewModel.onPhoneChange("081298765432")
        var successCalled = false

        viewModel.signUp { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!successCalled)
        assertEquals("Kata sandi minimal 6 karakter", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signUp_withInvalidIndonesianPhone_showsValidationError() = runTest {
        viewModel.onNameChange("Dewi Sartika")
        viewModel.onEmailChange("dewi@bantuin.test")
        viewModel.onPasswordChange("Rahasia123!")
        viewModel.onPhoneChange("12345")
        var successCalled = false

        viewModel.signUp { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!successCalled)
        assertEquals("Nomor telepon Indonesia tidak valid (contoh: 081234567890)", viewModel.uiState.value.errorMessage)
    }
}
