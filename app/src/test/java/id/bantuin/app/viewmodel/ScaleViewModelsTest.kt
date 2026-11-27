package id.bantuin.app.viewmodel

import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.RadiusExpansionState
import id.bantuin.app.domain.usecase.bookmark.BookmarkUseCase
import id.bantuin.app.ui.customer.providerdetail.ProviderDetailViewModel
import id.bantuin.app.ui.customer.recommendation.RecommendationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScaleViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun recommendationViewModel_bookmarkAndFilterFavorit() = runTest {
        val repo = FakeBantuinRepository()
        val bookmarkUseCase = BookmarkUseCase(mutableMapOf())
        val vm = RecommendationViewModel(
            catalogRepository = repo,
            bookmarkUseCase = bookmarkUseCase
        )
        vm.initialize(categoryId = 2, customerLocation = GeoPoint(-7.9797, 112.6304))
        advanceUntilIdle()

        val firstProvider = vm.uiState.value.filteredList.first().candidate
        vm.toggleBookmark(firstProvider)
        assertTrue(vm.isBookmarked(firstProvider.providerId))

        vm.setOnlyBookmarked(true)
        assertEquals(1, vm.uiState.value.filteredList.size)
        assertEquals(firstProvider.providerId, vm.uiState.value.filteredList.first().candidate.providerId)

        vm.setOnlyBookmarked(false)
        assertTrue(vm.uiState.value.filteredList.size >= 1)
    }

    @Test
    fun recommendationViewModel_dismissExpansionReturnsNormal() = runTest {
        val repo = FakeBantuinRepository()
        val vm = RecommendationViewModel(
            catalogRepository = repo,
            bookmarkUseCase = BookmarkUseCase(mutableMapOf())
        )
        // Lokasi ~14 km dari pusat Malang sehingga sebagian mitra di luar 10 km namun dalam 20 km
        vm.initialize(categoryId = 2, customerLocation = GeoPoint(-7.8550, 112.6304))
        advanceUntilIdle()

        if (vm.uiState.value.expansionState == RadiusExpansionState.SUGGESTING_EXPANSION) {
            vm.acceptRadiusExpansion()
            assertEquals(RadiusExpansionState.EXPANDED, vm.uiState.value.expansionState)
            assertEquals(20.0, vm.uiState.value.effectiveRadius, 0.01)
        }
        vm.dismissRadiusExpansion()
        assertEquals(RadiusExpansionState.NORMAL, vm.uiState.value.expansionState)
    }

    @Test
    fun providerDetailViewModel_toggleBookmarkUpdatesState() = runTest {
        val repo = FakeBantuinRepository()
        val bookmarkUseCase = BookmarkUseCase(mutableMapOf())
        val vm = ProviderDetailViewModel(
            catalogRepository = repo,
            reviewRepository = repo,
            bookmarkUseCase = bookmarkUseCase
        )
        val provider = FakeData.providers.first()
        vm.loadProviderDetail(provider.providerId, initialProvider = provider)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isBookmarked)
        vm.toggleBookmark(categoryId = 2)
        assertTrue(vm.uiState.value.isBookmarked)
        vm.toggleBookmark(categoryId = 2)
        assertFalse(vm.uiState.value.isBookmarked)
    }
}
