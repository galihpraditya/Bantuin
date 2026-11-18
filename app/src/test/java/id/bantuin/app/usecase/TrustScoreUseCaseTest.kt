package id.bantuin.app.usecase

import id.bantuin.app.domain.model.AnomalyReason
import id.bantuin.app.domain.model.CompletedJob
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class TrustScoreUseCaseTest {

    private val useCase = TrustScoreUseCase()
    private val baseInstant = Instant.fromEpochMilliseconds(1700000000000L)

    @Test
    fun testVector8_4_TrustScore() {
        // 1. Sehat: 8 SELESAI (6 tepat waktu, 2 telat), 2 DIBATALKAN_MITRA, 8 ulasan rata-rata 4.5
        // -> rating 90, on-time 75, cancel 20 -> 82.75, bukan anomali (20 tidak > 20), badge TRUSTED
        val completedSehat = (1..6).map {
            CompletedJob(startAt = baseInstant, startedAt = baseInstant + 10.minutes) // tepat waktu <= 15m
        } + (1..2).map {
            CompletedJob(startAt = baseInstant, startedAt = baseInstant + 25.minutes) // telat > 15m
        }
        val inputSehat = TrustInput(
            completed = completedSehat,
            mitraCancelledCount = 2,
            ratings = listOf(5, 4, 5, 4, 5, 4, 5, 4) // avg 4.5
        )
        val r1 = useCase.calculate(inputSehat)
        assertEquals(82.75, r1.trustScore, 0.001)
        assertFalse(r1.flagAnomaly)
        assertEquals(ProviderBadge.TRUSTED, r1.badge)

        // 2. Cancel tinggi: 6 SELESAI (semua tepat), 4 DIBATALKAN_MITRA, rata-rata 4.0
        // cancel 40 -> skor 83, anomali HIGH_CANCEL_RATE, badge NONE
        val completedCancel = (1..6).map {
            CompletedJob(startAt = baseInstant, startedAt = baseInstant + 5.minutes)
        }
        val inputCancel = TrustInput(
            completed = completedCancel,
            mitraCancelledCount = 4,
            ratings = listOf(4, 4, 4, 4, 4) // avg 4.0
        )
        val r2 = useCase.calculate(inputCancel)
        assertEquals(83.0, r2.trustScore, 0.001)
        assertTrue(r2.flagAnomaly)
        assertEquals(ProviderBadge.NONE, r2.badge)
        assertTrue(r2.reasons.contains(AnomalyReason.HIGH_CANCEL_RATE))

        // 3. Skor rendah: 5 SELESAI (1 tepat waktu), 0 batal, rata-rata 2.0
        // -> 18 + 7 + 20 = 45 -> anomali LOW_TRUST_SCORE
        val completedLow = listOf(
            CompletedJob(startAt = baseInstant, startedAt = baseInstant + 5.minutes)
        ) + (1..4).map {
            CompletedJob(startAt = baseInstant, startedAt = baseInstant + 30.minutes)
        }
        val inputLow = TrustInput(
            completed = completedLow,
            mitraCancelledCount = 0,
            ratings = listOf(2, 2, 2, 2, 2) // avg 2.0
        )
        val r3 = useCase.calculate(inputLow)
        assertEquals(45.0, r3.trustScore, 0.001)
        assertTrue(r3.flagAnomaly)
        assertTrue(r3.reasons.contains(AnomalyReason.LOW_TRUST_SCORE))

        // 4. Mitra baru: tx = 4 (< 5) -> trust 70, badge NEW, tanpa anomali
        val inputNew = TrustInput(
            completed = (1..4).map { CompletedJob(startAt = baseInstant, startedAt = baseInstant) },
            mitraCancelledCount = 0,
            ratings = listOf(5, 5, 5, 5)
        )
        val r4 = useCase.calculate(inputNew)
        assertEquals(70.0, r4.trustScore, 0.001)
        assertFalse(r4.flagAnomaly)
        assertEquals(ProviderBadge.NEW, r4.badge)

        // 5. Tanpa ulasan: tx >= 5, ratings kosong -> skor rating = 70
        val inputNoReviews = TrustInput(
            completed = (1..5).map { CompletedJob(startAt = baseInstant, startedAt = baseInstant) },
            mitraCancelledCount = 0,
            ratings = emptyList()
        )
        val r5 = useCase.calculate(inputNoReviews)
        assertEquals(70.0, r5.ratingScore, 0.001)
    }
}
