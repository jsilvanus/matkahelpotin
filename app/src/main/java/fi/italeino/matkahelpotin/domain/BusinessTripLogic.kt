package fi.italeino.matkahelpotin.domain

import java.time.LocalDate

data class BusinessTripSummary(
    val tripCount: Int,
    val legCount: Int,
    val distanceMeters: Long,
)

fun summarizeBusinessTrips(
    trips: List<BusinessTrip>,
    legs: List<BusinessTripLeg>,
    employmentId: EntityId,
    from: LocalDate? = null,
    untilExclusive: LocalDate? = null,
): BusinessTripSummary {
    val selected = trips.filter { trip ->
        trip.employmentId == employmentId &&
            (from == null || !trip.date.isBefore(from)) &&
            (untilExclusive == null || trip.date.isBefore(untilExclusive))
    }
    val ids = selected.map { it.id }.toSet()
    val selectedLegs = legs.filter { it.businessTripId in ids }
    return BusinessTripSummary(selected.size, selectedLegs.size, selectedLegs.sumOf { it.distanceMeters })
}

fun routeMatchesFirstLeg(trip: BusinessTrip, legs: List<BusinessTripLeg>, route: Route): Boolean {
    val first = legs.filter { it.businessTripId == trip.id }.minByOrNull { it.sequence } ?: return false
    return first.fromPlaceId == route.fromPlaceId && first.toPlaceId == route.toPlaceId
}

fun isReturnTrip(trip: BusinessTrip, legs: List<BusinessTripLeg>): Boolean =
    legs.count { it.businessTripId == trip.id } == 2
