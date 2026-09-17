package fi.italeino.matkahelpotin.domain

import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BusinessTripRoutingTest {
    @Test
    fun createsOneLegPerAddressPairAndPreservesProviderDistance() = runTest {
        val repository = RecordingBusinessTripRepository()
        val provider = FakeRoutingProvider()
        val trip = createRoutedBusinessTrip(
            repository = repository,
            date = LocalDate.of(2026, 9, 18),
            employmentId = UUID.randomUUID(),
            endpoints = listOf(
                RoutingEndpoint("Office, Pori"),
                RoutingEndpoint("Church, Pori"),
                RoutingEndpoint("Cemetery, Pori"),
            ),
            routingProvider = provider,
        )

        assertEquals(2, repository.legs.size)
        assertEquals(0, repository.legs[0].sequence)
        assertEquals(1, repository.legs[1].sequence)
        assertEquals(DistanceSource.ROUTING_PROVIDER, repository.legs[0].distanceSource)
        assertEquals(12_000, repository.legs[0].distanceMeters)
        assertEquals("fake-router", repository.legs[0].calculatedDistanceProvider)
        assertEquals("Church, Pori", repository.legs[0].toAddress)
        assertEquals(trip.id, repository.trip?.id)
    }

    @Test
    fun doesNotCreateAnythingForARejectedRoute() = runTest {
        val repository = RecordingBusinessTripRepository()
        val provider = FakeRoutingProvider(fail = true)

        assertFailsWith<IllegalStateException> {
            createRoutedBusinessTrip(
                repository = repository,
                date = LocalDate.of(2026, 9, 18),
                employmentId = UUID.randomUUID(),
                endpoints = listOf(RoutingEndpoint("A"), RoutingEndpoint("B")),
                routingProvider = provider,
            )
        }
        assertEquals(null, repository.trip)
        assertEquals(0, repository.legs.size)
    }

    private class FakeRoutingProvider(private val fail: Boolean = false) : RoutingProvider {
        override val id = "fake-router"

        override suspend fun route(request: RoutingRequest): RoutingResult {
            if (fail) error("route failed")
            return RoutingResult(
                distanceMeters = if (request.origin.address == "Office, Pori") 12_000 else 4_000,
                durationSeconds = 600,
                provider = id,
            )
        }
    }

    private class RecordingBusinessTripRepository : BusinessTripRepository {
        var trip: BusinessTrip? = null
        val legs = mutableListOf<BusinessTripLeg>()

        override fun observeAll() = kotlinx.coroutines.flow.flowOf(listOfNotNull(trip))
        override suspend fun upsert(trip: BusinessTrip) { this.trip = trip }
        override suspend fun delete(id: EntityId) {}
        override suspend fun createWithLegs(trip: BusinessTrip, legs: List<BusinessTripLeg>) {
            this.trip = trip
            this.legs += legs
        }
        override suspend fun deleteWithLegs(id: EntityId) {}
    }
}
