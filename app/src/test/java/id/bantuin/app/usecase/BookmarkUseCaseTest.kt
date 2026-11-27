package id.bantuin.app.usecase

import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.usecase.bookmark.BookmarkUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookmarkUseCaseTest {

    private lateinit var useCase: BookmarkUseCase

    private fun sampleCandidate(id: String) = ProviderCandidate(
        providerId = id,
        name = "Mitra $id",
        avatarUrl = null,
        location = GeoPoint(-7.9797, 112.6304),
        ratingAvg = 4.9,
        completionRate = 0.98,
        flagAnomaly = false,
        badge = ProviderBadge.TRUSTED,
        minBaseFare = 55_000
    )

    @Before
    fun setup() {
        useCase = BookmarkUseCase(mutableMapOf())
    }

    @Test
    fun toggleAddsAndRemovesBookmark() {
        val c1 = sampleCandidate("p1")
        assertTrue(useCase.toggle("cust_1", c1, categoryId = 2))
        assertTrue(useCase.isBookmarked("cust_1", "p1"))
        assertEquals(1, useCase.getBookmarks("cust_1").size)

        assertFalse(useCase.toggle("cust_1", c1, categoryId = 2))
        assertFalse(useCase.isBookmarked("cust_1", "p1"))
        assertTrue(useCase.getBookmarks("cust_1").isEmpty())
    }

    @Test
    fun bookmarksAreIsolatedPerCustomer() {
        val c1 = sampleCandidate("p1")
        useCase.toggle("cust_1", c1, categoryId = 2)

        assertTrue(useCase.isBookmarked("cust_1", "p1"))
        assertFalse(useCase.isBookmarked("cust_2", "p1"))
        assertTrue(useCase.getBookmarks("cust_2").isEmpty())
    }

    @Test
    fun clearAllRemovesCustomerBookmarks() {
        useCase.toggle("cust_1", sampleCandidate("p1"), categoryId = 1)
        useCase.toggle("cust_1", sampleCandidate("p2"), categoryId = 2)
        assertEquals(2, useCase.getBookmarks("cust_1").size)

        useCase.clearAll("cust_1")
        assertTrue(useCase.getBookmarks("cust_1").isEmpty())
    }
}
