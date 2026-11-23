package id.bantuin.app.usecase

import id.bantuin.app.domain.model.AnomalyReason
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustScoreUseCaseTest {
    private val calculate = TrustScoreUseCase()

    @Test
    fun formulaUsesCompletionAndCancellationDenominators() {
        val result = calculate(TrustInput(onTimeCount = 6, lateCount = 2, cancelledCount = 2,
            ratingCount = 8, ratingTotal = 36))
        assertEquals(90.0, result.ratingScore, 0.0001)
        assertEquals(75.0, result.onTimeRate, 0.0001)
        assertEquals(20.0, result.cancelRate, 0.0001)
        assertEquals(62.75, result.trustScore, 0.0001)
        assertFalse(result.flagAnomaly)
        assertEquals(ProviderBadge.TRUSTED, result.badge)

        val withUnfinished = calculate(TrustInput(onTimeCount = 1, cancelledCount = 1, otherCount = 3))
        assertEquals(20.0, withUnfinished.cancelRate, 0.0001)
        assertEquals(100.0, withUnfinished.onTimeRate, 0.0001)
        assertEquals(5, withUnfinished.txCount)
    }

    @Test
    fun aSingleAnomalyConditionIsSufficientAndBothCanBeReported() {
        val highCancel = calculate(TrustInput(onTimeCount = 6, cancelledCount = 4,
            ratingCount = 5, ratingTotal = 20))
        assertEquals(63.0, highCancel.trustScore, 0.0001)
        assertEquals(listOf(AnomalyReason.HIGH_CANCEL_RATE), highCancel.reasons)
        assertEquals(ProviderBadge.NONE, highCancel.badge)

        val lowScore = calculate(TrustInput(onTimeCount = 1, lateCount = 4,
            ratingCount = 5, ratingTotal = 10))
        assertEquals(25.0, lowScore.trustScore, 0.0001)
        assertEquals(listOf(AnomalyReason.LOW_TRUST_SCORE), lowScore.reasons)

        val both = calculate(TrustInput(lateCount = 1, cancelledCount = 1))
        assertEquals(setOf(AnomalyReason.LOW_TRUST_SCORE, AnomalyReason.HIGH_CANCEL_RATE), both.reasons.toSet())
    }

    @Test
    fun exactTrustThresholdAndSmallSamplesFollowTheSameRule() {
        val boundary = calculate(TrustInput(onTimeCount = 3, lateCount = 4,
            ratingCount = 1, ratingTotal = 5))
        assertEquals(60.0, boundary.trustScore, 0.0001)
        assertFalse(boundary.flagAnomaly)

        val onePositive = calculate(TrustInput(onTimeCount = 1, ratingCount = 1, ratingTotal = 5))
        assertEquals(80.0, onePositive.trustScore, 0.0001)
        assertEquals(ProviderBadge.TRUSTED, onePositive.badge)
    }

    @Test
    fun missingDataAndNegativeScoresAreClampedAndFinite() {
        val empty = calculate(TrustInput())
        assertEquals(0.0, empty.ratingScore, 0.0)
        assertEquals(0.0, empty.onTimeRate, 0.0)
        assertEquals(0.0, empty.cancelRate, 0.0)
        assertEquals(0.0, empty.trustScore, 0.0)
        assertTrue(empty.flagAnomaly)

        val cancelled = calculate(TrustInput(cancelledCount = 1))
        assertEquals(100.0, cancelled.cancelRate, 0.0)
        assertEquals(0.0, cancelled.trustScore, 0.0)
    }
}
