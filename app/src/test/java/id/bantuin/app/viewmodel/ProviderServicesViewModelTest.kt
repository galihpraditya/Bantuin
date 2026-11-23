package id.bantuin.app.viewmodel

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.provider.services.ProviderServicesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ProviderServicesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeCatalogRepository : CatalogRepository {
        val services = mutableListOf<ServiceItem>()
        val categories = listOf(
            ServiceCategory(1, "Pertukangan", "ic_build", 120, "Tukang"),
            ServiceCategory(2, "Teknisi AC", "ic_ac", 90, "AC & Elektronik")
        )
        var upsertCount = 0
        var deleteCount = 0

        override suspend fun categories(): AppResult<List<ServiceCategory>> = AppResult.Success(categories)
        override suspend fun candidates(categoryId: Int) = AppResult.Success(emptyList<id.bantuin.app.domain.model.ProviderCandidate>())
        override suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>> =
            AppResult.Success(services.filter { it.providerId == providerId })
        override suspend fun availability(providerId: String) = AppResult.Success(emptyList<id.bantuin.app.domain.model.DayAvailability>())
        override suspend fun saveAvailability(providerId: String, days: List<id.bantuin.app.domain.model.DayAvailability>) = AppResult.Success(Unit)

        override suspend fun upsertService(service: ServiceItem): AppResult<Unit> {
            upsertCount++
            services.removeAll { it.id == service.id }
            services.add(service)
            return AppResult.Success(Unit)
        }

        override suspend fun deleteService(serviceId: String): AppResult<Unit> {
            deleteCount++
            services.removeAll { it.id == serviceId }
            return AppResult.Success(Unit)
        }

        override suspend fun updateProviderProfile(
            providerId: String,
            name: String,
            bio: String,
            location: GeoPoint,
            address: String,
            phone: String
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    class FakeAuthRepository(private val user: UserProfile?) : AuthRepository {
        override suspend fun signIn(email: String, pass: String) = AppResult.Success("uid")
        override suspend fun signUp(name: String, email: String, pass: String, phone: String, isProvider: Boolean) = AppResult.Success("uid")
        override suspend fun signOut() = AppResult.Success(Unit)
        override fun activeRole(): Flow<String> = MutableStateFlow("provider")
        override suspend fun setActiveRole(role: String) {}
        override fun currentUser(): Flow<UserProfile?> = MutableStateFlow(user)
    }

    private lateinit var catalogRepo: FakeCatalogRepository
    private lateinit var authRepo: FakeAuthRepository
    private lateinit var viewModel: ProviderServicesViewModel

    private val providerUser = UserProfile(
        id = "p01",
        fullName = "Budi Santoso",
        email = "mitra@bantuin.test",
        phone = "08123456789",
        roles = setOf("provider")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        catalogRepo = FakeCatalogRepository()
        authRepo = FakeAuthRepository(providerUser)

        catalogRepo.services.add(
            ServiceItem(
                id = "s01",
                providerId = "p01",
                categoryId = 2,
                name = "Servis AC Standar",
                description = "Cuci AC dan pengecekan freon",
                baseFare = 75000,
                durationMin = 60,
                isActive = true
            )
        )

        viewModel = ProviderServicesViewModel(catalogRepo, authRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadData_loadsServicesAndCategories() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("p01", state.providerId)
        assertEquals(1, state.services.size)
        assertEquals("Servis AC Standar", state.services.first().name)
        assertEquals(2, state.categories.size)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun testSaveService_validService_savesAndClosesDialog() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.openAddDialog()
        assertTrue(viewModel.uiState.value.isUpsertDialogOpen)

        val newService = ServiceItem(
            id = "s02",
            providerId = "p01",
            categoryId = 2,
            name = "Bongkar Pasang AC",
            description = "Pindah unit AC",
            baseFare = 150000,
            durationMin = 120,
            isActive = true
        )
        viewModel.saveService(newService)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.services.size)
        assertFalse(viewModel.uiState.value.isUpsertDialogOpen)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testSaveService_validationBlankName_fails() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val invalidService = ServiceItem(
            id = "s_invalid",
            providerId = "p01",
            categoryId = 2,
            name = "   ",
            description = "Tanpa nama",
            baseFare = 75000,
            durationMin = 60,
            isActive = true
        )
        viewModel.saveService(invalidService)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, catalogRepo.upsertCount)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Nama layanan", ignoreCase = true))
    }

    @Test
    fun testSaveService_validationNegativeFare_fails() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val invalidService = ServiceItem(
            id = "s_invalid",
            providerId = "p01",
            categoryId = 2,
            name = "Layanan Murah",
            description = "",
            baseFare = -1000,
            durationMin = 60,
            isActive = true
        )
        viewModel.saveService(invalidService)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, catalogRepo.upsertCount)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Tarif dasar", ignoreCase = true))
    }

    @Test
    fun testSaveService_validationNonMultipleOf500_fails() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val invalidService = ServiceItem(
            id = "s_invalid",
            providerId = "p01",
            categoryId = 2,
            name = "Layanan Ganjil",
            description = "",
            baseFare = 75230,
            durationMin = 60,
            isActive = true
        )
        viewModel.saveService(invalidService)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, catalogRepo.upsertCount)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("500", ignoreCase = true))
    }

    @Test
    fun testSaveService_validationInvalidDuration_fails() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        // Kurang dari 15 menit
        val tooShort = ServiceItem(
            id = "s_short",
            providerId = "p01",
            categoryId = 2,
            name = "Kilat",
            description = "",
            baseFare = 50000,
            durationMin = 10,
            isActive = true
        )
        viewModel.saveService(tooShort)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, catalogRepo.upsertCount)
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Lebih dari 720 menit
        val tooLong = ServiceItem(
            id = "s_long",
            providerId = "p01",
            categoryId = 2,
            name = "Maraton",
            description = "",
            baseFare = 50000,
            durationMin = 800,
            isActive = true
        )
        viewModel.saveService(tooLong)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, catalogRepo.upsertCount)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testToggleServiceActive_invertsIsActive() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val original = viewModel.uiState.value.services.first()
        assertTrue(original.isActive)

        viewModel.toggleServiceActive(original)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = viewModel.uiState.value.services.first { it.id == original.id }
        assertFalse(updated.isActive)
    }

    @Test
    fun testDeleteService_removesItemAndReloads() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.services.size)

        viewModel.deleteService("s01")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, catalogRepo.deleteCount)
        assertEquals(0, viewModel.uiState.value.services.size)
    }
}
