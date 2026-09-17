package fi.italeino.matkahelpotin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.italeino.matkahelpotin.MatkahelpotinApplication
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
        viewModelScope.launch { businessLocationRepository.upsert(BusinessLocation(employmentId = employmentId, placeId = placeId, code = code)) }
    }

    fun addRoute(fromPlaceId: EntityId, toPlaceId: EntityId, distanceKm: String) {
        val meters = distanceKm.replace(',', '.').toDoubleOrNull()?.times(1000.0)?.toLong() ?: return
        if (meters <= 0L || fromPlaceId == toPlaceId) return
        viewModelScope.launch { routeRepository.upsert(Route(fromPlaceId = fromPlaceId, toPlaceId = toPlaceId, distanceMeters = meters, source = RouteSource.EMPLOYER_DEFINED)) }
    }

    fun addOneWay(date: LocalDate, employmentId: EntityId, route: Route, purpose: String? = null) {
        addMultiLeg(date, employmentId, listOf(route), purpose)
    }

    fun addReturn(date: LocalDate, employmentId: EntityId, outward: Route, reverse: Route?) {
        if (reverse == null) return
        addMultiLeg(date, employmentId, listOf(outward, reverse))
    }

    fun addMultiLeg(
        date: LocalDate,
        employmentId: EntityId,
        routes: List<Route>,
        purpose: String? = null,
        transportMode: TransportMode = TransportMode.PRIVATE_CAR,
    ) {
        if (routes.isEmpty()) return
        if (routes.zipWithNext().any { (current, next) -> current.toPlaceId != next.fromPlaceId }) return
        if (routes.any { it.distanceMeters < 0L }) return

        viewModelScope.launch {
            val tripId = UUID.randomUUID()
            val trip = BusinessTrip(
                id = tripId,
                date = date,
                employmentId = employmentId,
                purpose = purpose,
                transportMode = transportMode,
            )
            val legs = routes.mapIndexed { sequence, route ->
                BusinessTripLeg(
                    businessTripId = tripId,
                    sequence = sequence,
                    fromPlaceId = route.fromPlaceId,
                    toPlaceId = route.toPlaceId,
                    distanceMeters = route.distanceMeters,
                    distanceSource = route.source,
                    transportMode = transportMode,
                )
            }
            tripRepository.createWithLegs(trip, legs)
        }
    }

    fun removeOne(date: LocalDate, route: Route, returnOnly: Boolean = false) {
        viewModelScope.launch {
            val tripsNow = trips.first().filter { it.date == date }
            val legsNow = legs.first()
            val candidates = tripsNow.filter { trip ->
                val tripLegs = legsNow.filter { it.businessTripId == trip.id }.sortedBy { it.sequence }
                val matchesOutward = tripLegs.firstOrNull()?.let { it.fromPlaceId == route.fromPlaceId && it.toPlaceId == route.toPlaceId } == true
                if (returnOnly) tripLegs.size == 2 else tripLegs.size == 1
            }
            candidates.maxByOrNull { it.createdAt }?.let { trip ->
                tripRepository.deleteWithLegs(trip.id)
            }
        }
    }

    companion object {
        fun factory(application: MatkahelpotinApplication) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = BusinessViewModel(
                application.employmentRepository,
                application.placeRepository,
                application.businessLocationRepository,
                application.routeRepository,
                application.businessTripRepository,
                application.businessTripLegRepository,
            ) as T
        }
    }
}
