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
private fun CommuteProfileEntity.toDomain() = CommuteProfile(id, employmentId, homePlaceId, workplaceId, TransportMode.valueOf(transportMode), distanceMeters, ticketPriceCents, tripsPerDay, activeFrom, activeUntil, colourArgb, enabled)
private fun CommuteProfile.toEntity() = CommuteProfileEntity(id, employmentId, homePlaceId, workplaceId, transportMode.name, distanceMeters, ticketPriceCents, tripsPerDay, activeFrom, activeUntil, colourArgb, enabled)
private fun CommuteRecordEntity.toDomain() = CommuteRecord(id, date, commuteProfileId, tripCount, distanceMetersSnapshot, costCentsSnapshot, createdAt)
private fun CommuteRecord.toEntity() = CommuteRecordEntity(id, date, commuteProfileId, tripCount, distanceMetersSnapshot, costCentsSnapshot, createdAt)

class RoomEmploymentRepository(private val dao: EmploymentDao) : EmploymentRepository { override fun observeAll() = dao.observeAll().map { it.map(EmploymentEntity::toDomain) }; override suspend fun upsert(value: Employment) = dao.upsert(value.toEntity()) }
class RoomPlaceRepository(private val dao: PlaceDao) : PlaceRepository { override fun observeAll() = dao.observeAll().map { it.map(PlaceEntity::toDomain) }; override suspend fun upsert(value: Place) = dao.upsert(value.toEntity()) }
class RoomVehicleRepository(private val dao: VehicleDao) : VehicleRepository { override fun observeAll() = dao.observeAll().map { it.map(VehicleEntity::toDomain) }; override suspend fun upsert(value: Vehicle) = dao.upsert(value.toEntity()) }
class RoomEmploymentWorkplaceRepository(private val dao: EmploymentWorkplaceDao) : EmploymentWorkplaceRepository { override suspend fun upsert(value: EmploymentWorkplace) = dao.upsert(EmploymentWorkplaceEntity(value.employmentId, value.placeId, value.validFrom, value.validUntil)) }
class RoomCommuteProfileRepository(private val dao: CommuteProfileDao) : CommuteProfileRepository { override fun observeAll(): Flow<List<CommuteProfile>> = dao.observeAll().map { it.map(CommuteProfileEntity::toDomain) }; override suspend fun upsert(value: CommuteProfile) = dao.upsert(value.toEntity()) }
class RoomCommuteRecordRepository(private val dao: CommuteRecordDao) : CommuteRecordRepository { override fun observeAll(): Flow<List<CommuteRecord>> = dao.observeAll().map { it.map(CommuteRecordEntity::toDomain) }; override suspend fun upsert(value: CommuteRecord) = dao.upsert(value.toEntity()); override suspend fun delete(id: EntityId) = dao.delete(id) }
class RoomBusinessLocationRepository(private val dao: BusinessLocationDao) : BusinessLocationRepository { override suspend fun upsert(value: BusinessLocation) = dao.upsert(BusinessLocationEntity(value.id, value.employmentId, value.placeId, value.code, value.active)) }
