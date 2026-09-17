package fi.italeino.matkahelpotin.domain

import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class DomainValidationTest {
    @Test(expected = IllegalArgumentException::class)
    fun privateCarRequiresDistance() {
        validateCommuteProfile(
            CommuteProfile(
                employmentId = UUID.randomUUID(), homePlaceId = UUID.randomUUID(), workplaceId = UUID.randomUUID(),
                transportMode = TransportMode.PRIVATE_CAR
            )
        )
    }

    @Test
    fun publicTransportAllowsZeroTicketPrice() {
        validateCommuteProfile(
            CommuteProfile(
                employmentId = UUID.randomUUID(), homePlaceId = UUID.randomUUID(), workplaceId = UUID.randomUUID(),
                transportMode = TransportMode.PUBLIC_TRANSPORT, ticketPriceCents = 0
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun routeCannotHaveNegativeDistance() {
        validateRoute(Route(fromPlaceId = UUID.randomUUID(), toPlaceId = UUID.randomUUID(), distanceMeters = -1, source = RouteSource.MANUAL))
    }

    @Test
    fun routeKeepsEmployerSource() {
        val route = Route(fromPlaceId = UUID.randomUUID(), toPlaceId = UUID.randomUUID(), distanceMeters = 5_000, source = RouteSource.EMPLOYER_DEFINED)
        validateRoute(route)
        assert(route.source == RouteSource.EMPLOYER_DEFINED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun businessLegNeedsOrigin() {
        validateBusinessTripLeg(
            BusinessTripLeg(
                businessTripId = UUID.randomUUID(), sequence = 0, toAddress = "Destination", distanceMeters = 100,
                distanceSource = DistanceSource.MANUAL_OVERRIDE, manualDistanceOverrideMeters = 100, transportMode = TransportMode.PRIVATE_CAR
            )
        )
    }
}
