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
    suspend fun upsert(profile: CommuteProfile)
}

interface BusinessLocationRepository {
    suspend fun upsert(location: BusinessLocation)
}
