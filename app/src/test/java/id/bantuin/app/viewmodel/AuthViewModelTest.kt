package id.bantuin.app.viewmodel

import id.bantuin.app.data.repository.FakeBantuinRepository
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
        var returnedId: String? = null

        viewModel.signIn { id -> returnedId = id }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(returnedId)
        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signUp_withValidData_succeeds() = runTest {
        viewModel.onNameChange("Dewi Sartika")
        viewModel.onEmailChange("dewi@bantuin.test")
        viewModel.onPasswordChange("Rahasia123!")
        viewModel.onPhoneChange("081298765432")
        viewModel.onRoleToggle(true)
        var registeredId: String? = null

        viewModel.signUp { id -> registeredId = id }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(registeredId)
        assertTrue(viewModel.uiState.value.isSuccess)
    }
}
