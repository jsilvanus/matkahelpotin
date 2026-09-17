package fi.italeino.matkahelpotin.domain

import java.time.LocalDate
import java.util.UUID

/**
 * Creates a business trip from arbitrary addresses using a routing provider.
 * Employer-defined routes are deliberately not consulted or replaced.
 */
suspend fun createRoutedBusinessTrip(
    repository: BusinessTripRepository,
    date: LocalDate,
    employmentId: EntityId,
    endpoints: List<RoutingEndpoint>,
    routingProvider: RoutingProvider,
    transportMode: TransportMode = TransportMode.PRIVATE_CAR,
    purpose: String? = null,
): BusinessTrip {
    require(endpoints.size >= 2) { "A routed trip needs at least an origin and a destination" }
    endpoints.forEach { require(it.address.isNotBlank()) { "Routing addresses must not be blank" } }

    val tripId = UUID.randomUUID()
    val trip = BusinessTrip(
        id = tripId,
        date = date,
        employmentId = employmentId,
        purpose = purpose,
        transportMode = transportMode,
    )

    val legs = endpoints.zipWithNext().mapIndexed { sequence, (origin, destination) ->
        val result = routingProvider.route(
            RoutingRequest(
                origin = origin,
                destination = destination,
                transportMode = transportMode,
            )
        )
        require(result.provider == routingProvider.id) {
            "Routing result provider does not match the requested provider"
        }
        businessTripLegFromRouting(
            businessTripId = tripId,
            sequence = sequence,
            origin = origin,
            destination = destination,
            transportMode = transportMode,
            result = result,
        )
    }

    repository.createWithLegs(trip, legs)
    return trip
}
