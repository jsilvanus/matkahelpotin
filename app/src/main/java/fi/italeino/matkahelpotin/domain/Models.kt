package fi.italeino.matkahelpotin.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

typealias EntityId = UUID

enum class PlaceType { HOME, WORKPLACE, EMPLOYER_LOCATION, OTHER }
enum class TransportMode { PRIVATE_CAR, PUBLIC_TRANSPORT }
enum class RouteSource { EMPLOYER_DEFINED, MAP_PROVIDER, MANUAL }

enum class DistanceSource { EMPLOYER_DEFINED, ROUTING_PROVIDER, MANUAL_OVERRIDE }

data class Employment(
    val id: EntityId = UUID.randomUUID(),
    val employerName: String,
    val employerIdentifier: String? = null,
    val description: String? = null,
    val activeFrom: LocalDate? = null,
    val activeUntil: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
)

data class Place(
    val id: EntityId = UUID.randomUUID(),
    val name: String,
    val type: PlaceType,
    val address: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val country: String = "FI",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val source: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
)

data class EmploymentWorkplace(
    val id: EntityId = UUID.randomUUID(),
    val employmentId: EntityId,
    val placeId: EntityId,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
)

data class CommuteProfile(
    val id: EntityId = UUID.randomUUID(),
    val employmentId: EntityId,
    val homePlaceId: EntityId,
    val workplaceId: EntityId,
    val transportMode: TransportMode,
    val distanceMeters: Long? = null,
    val ticketPriceCents: Long? = null,
    val tripsPerDay: Int = 2,
    val activeFrom: LocalDate? = null,
    val activeUntil: LocalDate? = null,
    val colourArgb: Long = 0xFF6750A4,
    val enabled: Boolean = true,
)

data class CommuteRecord(
    val id: EntityId = UUID.randomUUID(),
    val date: LocalDate,
    val commuteProfileId: EntityId,
    val tripCount: Int,
    val distanceMetersSnapshot: Long? = null,
    val costCentsSnapshot: Long? = null,
    val createdAt: Instant = Instant.now(),
)

data class BusinessLocation(
    val id: EntityId = UUID.randomUUID(),
    val employmentId: EntityId,
    val placeId: EntityId,
    val code: String? = null,
    val active: Boolean = true,
)

data class Route(
    val id: EntityId = UUID.randomUUID(),
    val fromPlaceId: EntityId,
    val toPlaceId: EntityId,
    val distanceMeters: Long,
    val durationSeconds: Long? = null,
    val source: RouteSource,
    val effectiveFrom: LocalDate? = null,
    val effectiveUntil: LocalDate? = null,
)

data class BusinessTrip(
    val id: EntityId = UUID.randomUUID(),
    val date: LocalDate,
    val employmentId: EntityId,
    val purpose: String? = null,
    val transportMode: TransportMode = TransportMode.PRIVATE_CAR,
    val createdAt: Instant = Instant.now(),
)

data class BusinessTripLeg(
    val id: EntityId = UUID.randomUUID(),
    val businessTripId: EntityId,
    val sequence: Int,
    val fromPlaceId: EntityId? = null,
    val toPlaceId: EntityId? = null,
    val fromAddress: String? = null,
    val toAddress: String? = null,
    /** Effective distance used for calculations and reimbursement. */
    val distanceMeters: Long,
    val distanceSource: DistanceSource,
    /** Original routing-provider distance, retained when a manual override is applied. */
    val calculatedDistanceMeters: Long? = null,
    /** Stable provider identifier for the original calculated distance. */
    val calculatedDistanceProvider: String? = null,
    /** Explicit manual value when the effective distance is manually overridden. */
    val manualDistanceOverrideMeters: Long? = null,
    val transportMode: TransportMode,
)

data class Vehicle(
    val id: EntityId = UUID.randomUUID(),
    val name: String,
    val registration: String? = null,
    val active: Boolean = true,
)

data class ReimbursementRate(
    val id: EntityId = UUID.randomUUID(),
    val type: String,
    val amountCents: Long,
    val currency: String = "EUR",
    val unit: String = "km",
    val validFrom: LocalDate,
    val validUntil: LocalDate? = null,
    val jurisdiction: String = "FI",
)

data class MileagePolicy(
    val id: EntityId = UUID.randomUUID(),
    val year: Int,
    val mileageLimitMeters: Long? = null,
    val mileageRateCentsPerKm: Long,
    val jurisdiction: String = "FI",
)

fun validateCommuteProfile(profile: CommuteProfile) {
    require(profile.tripsPerDay > 0) { "tripsPerDay must be positive" }
    when (profile.transportMode) {
        TransportMode.PRIVATE_CAR -> require(profile.distanceMeters != null && profile.distanceMeters > 0) {
            "Private-car commute requires a positive distance"
        }
        TransportMode.PUBLIC_TRANSPORT -> require(profile.ticketPriceCents != null && profile.ticketPriceCents >= 0) {
            "Public-transport commute requires a ticket price"
        }
    }
}

fun validateRoute(route: Route) {
    require(route.distanceMeters >= 0) { "Route distance cannot be negative" }
}

fun validateBusinessTripLeg(leg: BusinessTripLeg) {
    require(leg.sequence >= 0) { "Leg sequence cannot be negative" }
    require(leg.distanceMeters >= 0) { "Leg distance cannot be negative" }
    require(leg.fromPlaceId != null || !leg.fromAddress.isNullOrBlank()) { "Leg needs an origin" }
    require(leg.toPlaceId != null || !leg.toAddress.isNullOrBlank()) { "Leg needs a destination" }

    when (leg.distanceSource) {
        DistanceSource.EMPLOYER_DEFINED -> {
            require(leg.calculatedDistanceMeters == null) { "Employer-defined distance cannot have a routing-provider distance" }
            require(leg.calculatedDistanceProvider == null) { "Employer-defined distance cannot have a routing provider" }
            require(leg.manualDistanceOverrideMeters == null) { "Employer-defined distance cannot have a manual override" }
        }
        DistanceSource.ROUTING_PROVIDER -> {
            require(leg.calculatedDistanceMeters != null && leg.calculatedDistanceMeters >= 0) { "Routing-provider distance must retain the calculated distance" }
            require(!leg.calculatedDistanceProvider.isNullOrBlank()) { "Routing-provider distance must identify its provider" }
            require(leg.distanceMeters == leg.calculatedDistanceMeters) { "Effective routing distance must equal the calculated distance" }
            require(leg.manualDistanceOverrideMeters == null) { "Routing-provider distance cannot have a manual override" }
        }
        DistanceSource.MANUAL_OVERRIDE -> {
            require(leg.manualDistanceOverrideMeters != null && leg.manualDistanceOverrideMeters >= 0) { "Manual override must contain a non-negative distance" }
            require(leg.distanceMeters == leg.manualDistanceOverrideMeters) { "Effective distance must equal the manual override" }
            if (leg.calculatedDistanceMeters != null) {
                require(leg.calculatedDistanceMeters >= 0) { "Original calculated distance cannot be negative" }
                require(!leg.calculatedDistanceProvider.isNullOrBlank()) { "Original calculated distance must identify its provider" }
            } else {
                require(leg.calculatedDistanceProvider == null) { "A provider requires an original calculated distance" }
            }
        }
    }
}

data class RoutingEndpoint(
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class RoutingRequest(
    val origin: RoutingEndpoint,
    val destination: RoutingEndpoint,
    val transportMode: TransportMode,
)

data class RoutingResult(
    val distanceMeters: Long,
    val durationSeconds: Long? = null,
    val provider: String,
) {
    init {
        require(distanceMeters >= 0) { "Routing distance cannot be negative" }
        require(provider.isNotBlank()) { "Routing provider must not be blank" }
    }
}

interface RoutingProvider {
    val id: String
    suspend fun route(request: RoutingRequest): RoutingResult
}

fun BusinessTripLeg.withManualDistanceOverride(distanceMeters: Long): BusinessTripLeg {
    require(distanceMeters >= 0) { "Manual distance override cannot be negative" }
    return copy(
        distanceMeters = distanceMeters,
        distanceSource = DistanceSource.MANUAL_OVERRIDE,
        manualDistanceOverrideMeters = distanceMeters,
    ).also(::validateBusinessTripLeg)
}

fun businessTripLegFromRouting(
    businessTripId: EntityId,
    sequence: Int,
    origin: RoutingEndpoint,
    destination: RoutingEndpoint,
    transportMode: TransportMode,
    result: RoutingResult,
    fromPlaceId: EntityId? = null,
    toPlaceId: EntityId? = null,
): BusinessTripLeg =
    BusinessTripLeg(
        businessTripId = businessTripId,
        sequence = sequence,
        fromPlaceId = fromPlaceId,
        toPlaceId = toPlaceId,
        fromAddress = origin.address,
        toAddress = destination.address,
        distanceMeters = result.distanceMeters,
        distanceSource = DistanceSource.ROUTING_PROVIDER,
        calculatedDistanceMeters = result.distanceMeters,
        calculatedDistanceProvider = result.provider,
        transportMode = transportMode,
    ).also(::validateBusinessTripLeg)
