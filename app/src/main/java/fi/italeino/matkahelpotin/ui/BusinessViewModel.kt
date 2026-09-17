package fi.italeino.matkahelpotin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.italeino.matkahelpotin.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class BusinessViewModel(
    private val employmentRepository: EmploymentRepository,
    private val placeRepository: PlaceRepository,
    private val businessLocationRepository: BusinessLocationRepository,
    private val routeRepository: RouteRepository,
    private val tripRepository: BusinessTripRepository,
    private val legRepository: BusinessTripLegRepository,
) : ViewModel() {
    val employments: Flow<List<Employment>> = employmentRepository.observeAll()
    val places: Flow<List<Place>> = placeRepository.observeAll()
    val businessLocations: Flow<List<BusinessLocation>> = businessLocationRepository.observeAll()
    val routes: Flow<List<Route>> = routeRepository.observeAll()
    val trips: Flow<List<BusinessTrip>> = tripRepository.observeAll()
    val legs: Flow<List<BusinessTripLeg>> = legRepository.observeAll()

    fun addBusinessLocation(employmentId: EntityId, placeId: EntityId, code: String?) {
        viewModelScope.launch {
            businessLocationRepository.upsert(BusinessLocation(employmentId = employmentId, placeId = placeId, code = code))
        }
    }

    fun addRoute(fromPlaceId: EntityId, toPlaceId: EntityId, distanceKm: String) {
        val meters = distanceKm.replace(',', '.').toDoubleOrNull()?.times(1000.0)?.toLong() ?: return
        if (meters <= 0L || fromPlaceId == toPlaceId) return
        viewModelScope.launch {
            routeRepository.upsert(Route(fromPlaceId = fromPlaceId, toPlaceId = toPlaceId, distanceMeters = meters, source = RouteSource.EMPLOYER_DEFINED))
        }
    }

    fun addOneWay(date: LocalDate, employmentId: EntityId, route: Route, purpose: String? = null) {
        viewModelScope.launch {
            val tripId = UUID.randomUUID()
            tripRepository.upsert(BusinessTrip(id = tripId, date = date, employmentId = employmentId, purpose = purpose))
            legRepository.upsert(
                BusinessTripLeg(
                    businessTripId = tripId,
                    sequence = 0,
                    fromPlaceId = route.fromPlaceId,
                    toPlaceId = route.toPlaceId,
                    distanceMeters = route.distanceMeters,
                    distanceSource = route.source,
                    transportMode = TransportMode.PRIVATE_CAR,
                )
            )
        }
    }

    fun addReturn(date: LocalDate, employmentId: EntityId, outward: Route, reverse: Route?) {
        if (reverse == null) return
        viewModelScope.launch {
            val tripId = UUID.randomUUID()
            tripRepository.upsert(BusinessTrip(id = tripId, date = date, employmentId = employmentId))
            legRepository.upsert(
                BusinessTripLeg(
                    businessTripId = tripId, sequence = 0,
                    fromPlaceId = outward.fromPlaceId, toPlaceId = outward.toPlaceId,
                    distanceMeters = outward.distanceMeters, distanceSource = outward.source,
                    transportMode = TransportMode.PRIVATE_CAR,
                )
            )
            legRepository.upsert(
                BusinessTripLeg(
                    businessTripId = tripId, sequence = 1,
                    fromPlaceId = reverse.fromPlaceId, toPlaceId = reverse.toPlaceId,
                    distanceMeters = reverse.distanceMeters, distanceSource = reverse.source,
                    transportMode = TransportMode.PRIVATE_CAR,
                )
            )
        }
    }

    fun removeOne(date: LocalDate, route: Route, returnOnly: Boolean = false) {
        viewModelScope.launch {
            val tripsNow = trips.first().filter { it.date == date }
            val legsNow = legs.first()
            val candidates = tripsNow.filter { trip ->
                val tripLegs = legsNow.filter { it.businessTripId == trip.id }.sortedBy { it.sequence }
                val matchesOutward = tripLegs.firstOrNull()?.let { it.fromPlaceId == route.fromPlaceId && it.toPlaceId == route.toPlaceId } == true
                if (!matchesOutward) return@filter false
                if (!returnOnly) return@filter true
                tripLegs.size == 2
            }
            candidates.maxByOrNull { it.createdAt }?.let { trip ->
                legRepository.deleteForTrip(trip.id)
                tripRepository.delete(trip.id)
            }
        }
    }
}
