package fi.italeino.matkahelpotin.domain

import kotlinx.coroutines.flow.Flow

interface EmploymentRepository {
    fun observeAll(): Flow<List<Employment>>
    suspend fun upsert(employment: Employment)
}

interface PlaceRepository {
    fun observeAll(): Flow<List<Place>>
    suspend fun upsert(place: Place)
}

interface VehicleRepository {
    fun observeAll(): Flow<List<Vehicle>>
    suspend fun upsert(vehicle: Vehicle)
}

interface EmploymentWorkplaceRepository {
    suspend fun upsert(workplace: EmploymentWorkplace)
}

interface CommuteProfileRepository {
    fun observeAll(): Flow<List<CommuteProfile>>
    suspend fun upsert(profile: CommuteProfile)
}

interface CommuteRecordRepository {
    fun observeAll(): Flow<List<CommuteRecord>>
    suspend fun upsert(record: CommuteRecord)
    suspend fun delete(id: EntityId)
}

interface BusinessLocationRepository {
    fun observeAll(): Flow<List<BusinessLocation>>
    suspend fun upsert(location: BusinessLocation)
}

interface RouteRepository {
    fun observeAll(): Flow<List<Route>>
    suspend fun upsert(route: Route)
}

interface BusinessTripRepository {
    fun observeAll(): Flow<List<BusinessTrip>>
    suspend fun upsert(trip: BusinessTrip)
    suspend fun delete(id: EntityId)
    suspend fun createWithLegs(trip: BusinessTrip, legs: List<BusinessTripLeg>)
    suspend fun deleteWithLegs(id: EntityId)
}

interface BusinessTripLegRepository {
    fun observeAll(): Flow<List<BusinessTripLeg>>
    suspend fun upsert(leg: BusinessTripLeg)
    suspend fun deleteForTrip(tripId: EntityId)
}

interface ReimbursementRateRepository {
    fun observeAll(): Flow<List<ReimbursementRate>>
    suspend fun upsert(rate: ReimbursementRate)
}

interface MileagePolicyRepository {
    fun observeAll(): Flow<List<MileagePolicy>>
    suspend fun upsert(policy: MileagePolicy)
}
