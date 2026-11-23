package id.bantuin.app.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.CompletionStatus
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.TransactionOutcome
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import id.bantuin.app.domain.model.ProviderCandidate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBantuinRepositoryTest {
    private lateinit var repo: FakeBantuinRepository

    @Before
    fun setUp() {
        repo = FakeBantuinRepository(sessionStore = InMemorySessionStore(initialUser = null))
    }

    @Test
    fun signIn_withWrongCredentials_isRejected() = runBlocking {
        val result = repo.signIn("pelanggan1@bantuin.test", "salah")

        assertTrue(result is AppResult.Error)
        assertEquals(null, repo.currentUser().firstOrNull())
    }

    @Test
    fun signIn_withMitraCredentials_returnsMitraProfile() = runBlocking {
        val result = repo.signIn("mitra01@bantuin.test", "Demo12345!")

        assertTrue(result is AppResult.Success)
        val user = repo.currentUser().firstOrNull()
        assertEquals("p01", user?.id)
        assertEquals("Budi Santoso, S.T.", user?.fullName)
        assertEquals(setOf("provider"), user?.roles)
    }

    @Test
    fun confirmationUpsertsReviewAndChangesRecommendationPriority() = runBlocking {
        repo.signIn("pelanggan1@bantuin.test", "Demo12345!")
        val before = candidate("p01")
        assertEquals(ProviderBadge.TRUSTED, before.badge)
        assertTrue(repo.confirmCompletion("b01", "p01", CompletionStatus.TERLAMBAT, 1, "Terlambat", emptyList())
            is AppResult.Success)
        val first = (repo.bookingDetails("b01") as AppResult.Success<BookingItem?>).data!!
        assertEquals(BookingStatus.SELESAI, first.status)
        assertEquals(CompletionStatus.TERLAMBAT, first.outcome?.status)
        assertEquals(1, first.review?.rating)
        assertTrue(candidate("p01").flagAnomaly)

        val flagged = candidate("p01")
        val matchmaking = MatchmakingUseCase()
        val origin = GeoPoint(-7.965, 112.632)
        val penalized = matchmaking(origin, listOf(flagged)).ranked.single().finalScore
        val normal = matchmaking(origin, listOf(flagged.copy(flagAnomaly = false))).ranked.single().finalScore
        assertEquals(normal * 0.7, penalized, 0.0001)

        assertTrue(repo.confirmCompletion("b01", "p01", CompletionStatus.SELESAI_TEPAT_WAKTU, 5, "Sudah dikoreksi",
            listOf("Tepat Waktu")) is AppResult.Success)
        val updated = (repo.bookingDetails("b01") as AppResult.Success<BookingItem?>).data!!
        assertEquals(first.review?.id, updated.review?.id)
        assertEquals(first.review?.createdAt, updated.review?.createdAt)
        assertEquals(first.outcome?.resolvedAt, updated.outcome?.resolvedAt)
        assertEquals(ProviderBadge.TRUSTED, candidate("p01").badge)

        repo.deleteReview("b01")
        val afterDeletion = (repo.bookingDetails("b01") as AppResult.Success<BookingItem?>).data!!
        assertNull(afterDeletion.review)
        assertEquals(CompletionStatus.SELESAI_TEPAT_WAKTU, afterDeletion.outcome?.status)
        repo.deleteTransactionOutcome("b01")
        val afterReset = (repo.bookingDetails("b01") as AppResult.Success<BookingItem?>).data!!
        assertEquals(false, afterReset.outcome?.customerConfirmed)
        assertEquals(BookingStatus.SELESAI, afterReset.status)
    }

    @Test
    fun historyAndInputsAreScopedToProviderWindowAndStatus() = runBlocking {
        var now = Instant.parse("2026-06-01T00:00:00Z")
        val clock = object : Clock { override fun now() = now }
        val clockedRepo = FakeBantuinRepository(clock = clock)
        val cancellations = (clockedRepo.transactionHistory("p05", 30, CompletionStatus.DIBATALKAN)
            as AppResult.Success<List<TransactionOutcome>>).data
        assertEquals(1, cancellations.size)
        assertEquals("p05", cancellations.single().providerId)
        assertEquals(0, (clockedRepo.trustInput("p01", 30) as AppResult.Success<TrustInput>).data.cancelledCount)
        now += 31.days
        assertEquals(TrustInput(), (clockedRepo.trustInput("p05", 30) as AppResult.Success<TrustInput>).data)
        assertTrue((clockedRepo.transactionHistory("p05", 30) as AppResult.Success<List<TransactionOutcome>>).data.isEmpty())
    }

    private suspend fun candidate(id: String): ProviderCandidate =
        (repo.candidates(2) as AppResult.Success<List<ProviderCandidate>>).data.single { it.providerId == id }

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
        assertEquals("Teknisi AC bersertifikat 10 tahun", candidate.bio)
        assertEquals("Jl. Sukarno Hatta No. 45, Malang", candidate.address)
        assertEquals(newLoc.lat, candidate.location.lat, 0.0001)
    }
}
