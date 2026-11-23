package id.bantuin.app.viewmodel

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.ReviewRepository
import id.bantuin.app.ui.customer.providerdetail.ProviderDetailViewModel
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
class ProviderDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBantuinRepository
    private lateinit var viewModel: ProviderDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBantuinRepository()
        viewModel = ProviderDetailViewModel(fakeRepository, fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsProviderDetail_withProvidedInitialCandidate() = runTest {
        val initial = FakeData.providers.first { it.providerId == "p01" }
        viewModel.loadProviderDetail("p01", initial)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("p01", state.provider?.providerId)
        assertEquals("Budi Santoso, S.T.", state.provider?.name)
        assertTrue(state.services.isNotEmpty())
        assertTrue(state.services.all { it.providerId == "p01" && it.isActive })
        assertTrue(state.reviews.isNotEmpty())
    }

    @Test
    fun resolvesProviderCandidate_whenInitialCandidateIsNull() = runTest {
        viewModel.loadProviderDetail("p02", initialProvider = null)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.provider)
        assertEquals("p02", state.provider?.providerId)
        assertEquals("Ahmad Fauzi", state.provider?.name)
    }

    @Test
    fun handlesEmptyServices_gracefully() = runTest {
        val emptyServicesRepo = object : CatalogRepository by fakeRepository {
            override suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>> {
                return AppResult.Success(emptyList())
            }
        }
        val vm = ProviderDetailViewModel(emptyServicesRepo, fakeRepository)
        vm.loadProviderDetail("p01")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.services.isEmpty())
        assertTrue(state.reviews.isNotEmpty())
    }

    @Test
    fun handlesEmptyReviews_gracefully() = runTest {
        val emptyReviewsRepo = object : ReviewRepository by fakeRepository {
            override suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>> {
                return AppResult.Success(emptyList())
            }
        }
        val vm = ProviderDetailViewModel(fakeRepository, emptyReviewsRepo)
        vm.loadProviderDetail("p01")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.services.isNotEmpty())
        assertTrue(state.reviews.isEmpty())
    }
}
