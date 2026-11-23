package id.bantuin.app.viewmodel

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import id.bantuin.app.ui.customer.recommendation.RecommendationViewModel
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
class RecommendationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBantuinRepository
    private lateinit var viewModel: RecommendationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBantuinRepository()
        viewModel = RecommendationViewModel(fakeRepository, MatchmakingUseCase())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialization_loadsCategoryAndRankedCandidates() = runTest {
        viewModel.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.categoryId)
        assertEquals("Teknisi AC & Elektronik", state.category.name)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.filteredList.isNotEmpty())
        assertTrue(state.matchResult.ranked.isNotEmpty())

        // Pastikan urutan Match Score menurun
        val scores = state.filteredList.map { it.matchScore }
        assertEquals(scores.sortedDescending(), scores)
    }

    @Test
    fun filtering_byMaxDistance_filtersOutDistantCandidates() = runTest {
        viewModel.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        val originalSize = viewModel.uiState.value.filteredList.size

        // Terapkan filter jarak <= 3.0 km
        viewModel.setMaxDistanceFilter(3.0)

        val filteredState = viewModel.uiState.value
        assertEquals(3.0, filteredState.maxDistanceFilter, 0.001)
        assertTrue(filteredState.filteredList.all { it.distanceKm <= 3.0 })
        assertTrue(filteredState.filteredList.size <= originalSize)
    }

    @Test
    fun filtering_byMinRating_filtersOutLowRatedCandidates() = runTest {
        viewModel.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        // Terapkan filter rating >= 4.8
        viewModel.setMinRatingFilter(4.8)

        val filteredState = viewModel.uiState.value
        assertEquals(4.8, filteredState.minRatingFilter, 0.001)
        assertTrue(filteredState.filteredList.all { (it.candidate.ratingAvg ?: 0.0) >= 4.8 })
    }

    @Test
    fun filtering_onlyTrusted_filtersOutNonTrustedCandidates() = runTest {
        viewModel.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setOnlyTrusted(true)

        val filteredState = viewModel.uiState.value
        assertTrue(filteredState.onlyTrusted)
        assertTrue(filteredState.filteredList.all { it.candidate.badge == ProviderBadge.TRUSTED })
    }

    @Test
    fun resetFilters_restoresDefaults() = runTest {
        viewModel.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        val fullCount = viewModel.uiState.value.filteredList.size

        // Pasang filter ketat
        viewModel.setFilters(maxDist = 2.0, minRating = 4.9, onlyTrusted = true)
        val strictCount = viewModel.uiState.value.filteredList.size

        // Reset
        viewModel.resetFilters()

        val resetState = viewModel.uiState.value
        assertEquals(10.0, resetState.maxDistanceFilter, 0.001)
        assertEquals(0.0, resetState.minRatingFilter, 0.001)
        assertFalse(resetState.onlyTrusted)
        assertEquals(fullCount, resetState.filteredList.size)
        assertTrue(fullCount >= strictCount)
    }

    @Test
    fun setFilters_updatesAllFiltersAtomically() = runTest {
        viewModel.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilters(maxDist = 5.0, minRating = 4.5, onlyTrusted = false)

        val state = viewModel.uiState.value
        assertEquals(5.0, state.maxDistanceFilter, 0.001)
        assertEquals(4.5, state.minRatingFilter, 0.001)
        assertFalse(state.onlyTrusted)
        assertTrue(state.filteredList.all { it.distanceKm <= 5.0 && (it.candidate.ratingAvg ?: 0.0) >= 4.5 })
    }

    @Test
    fun errorHandling_whenRepositoryFails() = runTest {
        val errorRepo = object : CatalogRepository by fakeRepository {
            override suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>> {
                return AppResult.Error("Koneksi jaringan terputus")
            }
        }
        val errorVm = RecommendationViewModel(errorRepo, MatchmakingUseCase())
        errorVm.initialize(categoryId = 2, customerLocation = FakeData.MALANG_CENTER)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = errorVm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Koneksi jaringan terputus", state.errorMessage)
        assertTrue(state.filteredList.isEmpty())
    }
}
