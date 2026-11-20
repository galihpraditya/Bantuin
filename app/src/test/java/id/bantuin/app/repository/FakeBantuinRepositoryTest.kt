package id.bantuin.app.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderCandidate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBantuinRepositoryTest {
    private lateinit var repo: FakeBantuinRepository

    @Before
    fun setUp() {
        repo = FakeBantuinRepository()
    }

    @Test
    fun submitReview_triggersRecalculateMetrics_andUpdatesProviderList() = runBlocking {
        val initialResult = repo.candidates(2)
        assertTrue(initialResult is AppResult.Success<List<ProviderCandidate>>)
        val initialCandidate = (initialResult as AppResult.Success<List<ProviderCandidate>>).data.first { it.providerId == "p01" }
        val initialRating = initialCandidate.ratingAvg ?: 0.0

        repo.submitReview(
            bookingId = "b01",
            providerId = "p01",
            rating = 5,
            comment = "Layanan istimewa dan cepat",
            tags = listOf("Cepat", "Rapi")
        )

        val updatedResult = repo.candidates(2)
        assertTrue(updatedResult is AppResult.Success<List<ProviderCandidate>>)
        val updatedCandidate = (updatedResult as AppResult.Success<List<ProviderCandidate>>).data.first { it.providerId == "p01" }
        assertNotNull(updatedCandidate.ratingAvg)
        assertTrue(updatedCandidate.ratingAvg!! >= initialRating)
    }

    @Test
    fun updateProviderProfile_updatesLocationAndBio() = runBlocking {
        val newLoc = GeoPoint(-7.9500, 112.6200)
        val res = repo.updateProviderProfile(
            providerId = "p01",
            name = "Budi Santoso Baru",
            bio = "Teknisi AC bersertifikat 10 tahun",
            location = newLoc,
            address = "Jl. Sukarno Hatta No. 45, Malang",
            phone = "081299998888"
        )
        assertTrue(res is AppResult.Success<Unit>)

        val candidateResult = repo.candidates(2)
        assertTrue(candidateResult is AppResult.Success<List<ProviderCandidate>>)
        val candidate = (candidateResult as AppResult.Success<List<ProviderCandidate>>).data.first { it.providerId == "p01" }
        assertEquals("Budi Santoso Baru", candidate.name)
        assertEquals(newLoc.lat, candidate.location.lat, 0.0001)
    }
}
