package id.bantuin.app.usecase

import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RadiusExpansionState
import id.bantuin.app.domain.usecase.matchmaking.DynamicRadiusExpansionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicRadiusExpansionUseCaseTest {

    private val useCase = DynamicRadiusExpansionUseCase()
    private val center = GeoPoint(-7.9797, 112.6304)

    private fun candidateAt(id: String, lat: Double, lng: Double) = ProviderCandidate(
        providerId = id,
        name = "Mitra $id",
        avatarUrl = null,
        location = GeoPoint(lat, lng),
        ratingAvg = 4.8,
        completionRate = 0.95,
        flagAnomaly = false,
        badge = ProviderBadge.TRUSTED,
        minBaseFare = 50_000
    )

    @Test
    fun normalState_whenProviderExistsWithinDefaultRadius() {
        val nearProvider = candidateAt("p1", -7.9800, 112.6310) // < 1 km
        val result = useCase(center, listOf(nearProvider))

        assertEquals(RadiusExpansionState.NORMAL, result.expansionState)
        assertEquals(10.0, result.effectiveRadiusKm, 0.01)
        assertEquals(1, result.matchResult.ranked.size)
        assertEquals(0, result.potentialCountIfExpanded)
    }

    @Test
    fun suggestsExpansion_whenNoneInDefaultRadiusButSomeIn20Km() {
        // ~14 km north of Malang center
        val midRangeProvider = candidateAt("p2", -7.8550, 112.6304)
        val result = useCase(center, listOf(midRangeProvider))

        assertEquals(RadiusExpansionState.SUGGESTING_EXPANSION, result.expansionState)
        assertEquals(10.0, result.effectiveRadiusKm, 0.01)
        assertTrue(result.matchResult.ranked.isEmpty())
        assertEquals(1, result.potentialCountIfExpanded)
    }

    @Test
    fun expandedState_returnsExpandedProvidersWhenAllowed() {
        val midRangeProvider = candidateAt("p2", -7.8550, 112.6304)
        val result = useCase(center, listOf(midRangeProvider), allowExpanded = true)

        assertEquals(RadiusExpansionState.EXPANDED, result.expansionState)
        assertEquals(20.0, result.effectiveRadiusKm, 0.01)
        assertEquals(1, result.matchResult.ranked.size)
    }

    @Test
    fun normalState_whenNoProvidersEvenInExpandedRadius() {
        // ~50 km away
        val farProvider = candidateAt("p3", -7.5000, 112.6304)
        val result = useCase(center, listOf(farProvider))

        assertEquals(RadiusExpansionState.NORMAL, result.expansionState)
        assertEquals(0, result.potentialCountIfExpanded)
    }
}
