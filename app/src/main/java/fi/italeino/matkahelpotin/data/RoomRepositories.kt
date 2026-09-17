package fi.italeino.matkahelpotin.data

import fi.italeino.matkahelpotin.data.local.*
import fi.italeino.matkahelpotin.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun EmploymentEntity.toDomain() = Employment(id, employerName, employerIdentifier, description, activeFrom, activeUntil, createdAt, updatedAt)
private fun Employment.toEntity() = EmploymentEntity(id, employerName, employerIdentifier, description, activeFrom, activeUntil, createdAt, updatedAt)
private fun PlaceEntity.toDomain() = Place(id, name, PlaceType.valueOf(type), address, postalCode, city, country, latitude, longitude, source, createdAt, updatedAt)
private fun Place.toEntity() = PlaceEntity(id, name, type.name, address, postalCode, city, country, latitude, longitude, source, createdAt, updatedAt)
private fun VehicleEntity.toDomain() = Vehicle(id, name, registration, active)
private fun Vehicle.toEntity() = VehicleEntity(id, name, registration, active)

class RoomEmploymentRepository(private val dao: EmploymentDao) : EmploymentRepository {
    override fun observeAll(): Flow<List<Employment>> = dao.observeAll().map { it.map(EmploymentEntity::toDomain) }
    override suspend fun upsert(employment: Employment) = dao.upsert(employment.toEntity())
}

class RoomPlaceRepository(private val dao: PlaceDao) : PlaceRepository {
    override fun observeAll(): Flow<List<Place>> = dao.observeAll().map { it.map(PlaceEntity::toDomain) }
    override suspend fun upsert(place: Place) = dao.upsert(place.toEntity())
}

class RoomVehicleRepository(private val dao: VehicleDao) : VehicleRepository {
    override fun observeAll(): Flow<List<Vehicle>> = dao.observeAll().map { it.map(VehicleEntity::toDomain) }
    override suspend fun upsert(vehicle: Vehicle) = dao.upsert(vehicle.toEntity())
}

class RoomEmploymentWorkplaceRepository(private val dao: EmploymentWorkplaceDao) : EmploymentWorkplaceRepository {
    override suspend fun upsert(workplace: EmploymentWorkplace) = dao.upsert(
        EmploymentWorkplaceEntity(workplace.employmentId, workplace.placeId, workplace.validFrom, workplace.validUntil)
    )
}

class RoomCommuteProfileRepository(private val dao: CommuteProfileDao) : CommuteProfileRepository {
    override suspend fun upsert(profile: CommuteProfile) = dao.upsert(
        CommuteProfileEntity(profile.id, profile.employmentId, profile.homePlaceId, profile.workplaceId, profile.transportMode.name,
            profile.distanceMeters, profile.ticketPriceCents, profile.tripsPerDay, profile.activeFrom, profile.activeUntil, profile.colourArgb, profile.enabled)
    )
}

class RoomBusinessLocationRepository(private val dao: BusinessLocationDao) : BusinessLocationRepository {
    override suspend fun upsert(location: BusinessLocation) = dao.upsert(
        BusinessLocationEntity(location.id, location.employmentId, location.placeId, location.code, location.active)
    )
}
