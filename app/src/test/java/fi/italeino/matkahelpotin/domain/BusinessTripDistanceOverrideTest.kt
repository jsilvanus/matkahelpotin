package fi.italeino.matkahelpotin.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class BusinessTripDistanceOverrideTest {
    @Test
    fun overridePersistsEffectiveDistanceAndRetainsRoutingProvenance() = kotlinx.coroutines.runBlocking {
        val tripId = UUID.randomUUID()
        val leg = businessTripLegFromRouting(
            businessTripId = tripId,
            sequence = 0,
            origin = RoutingEndpoint("A"),
            destination = RoutingEndpoint("B"),
            transportMode = TransportMode.PRIVATE_CAR,
            result = RoutingResult(12500, provider = "test-router"),
        )
        val repository = FakeLegRepository()

        val updated = overrideBusinessTripLegDistance(repository, leg, 11000)

        assertEquals(DistanceSource.MANUAL_OVERRIDE, updated.distanceSource)
        assertEquals(11000, updated.distanceMeters)
        assertEquals(12500, updated.calculatedDistanceMeters)
        assertEquals("test-router", updated.calculatedDistanceProvider)
        assertEquals(updated, repository.last)
    }

    @Test(expected = IllegalArgumentException::class)
    fun overrideRejectsNegativeDistance() = kotlinx.coroutines.runBlocking {
        val leg = businessTripLegFromRouting(
            businessTripId = UUID.randomUUID(),
            sequence = 0,
            origin = RoutingEndpoint("A"),
            destination = RoutingEndpoint("B"),
            transportMode = TransportMode.PRIVATE_CAR,
            result = RoutingResult(12500, provider = "test-router"),
        )
        overrideBusinessTripLegDistance(FakeLegRepository(), leg, -1)
    }

    private class FakeLegRepository : BusinessTripLegRepository {
        private val state = MutableStateFlow<List<BusinessTripLeg>>(emptyList())
        var last: BusinessTripLeg? = null

        override fun observeAll(): Flow<List<BusinessTripLeg>> = state
        override suspend fun upsert(leg: BusinessTripLeg) {
            last = leg
            state.value = listOf(leg)
        }
        override suspend fun deleteForTrip(tripId: EntityId) {
            state.value = state.value.filterNot { it.businessTripId == tripId }
        }
    }
}
