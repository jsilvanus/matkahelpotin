package fi.italeino.matkahelpotin.data

import androidx.room.withTransaction
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
private fun BusinessLocationEntity.toDomain() = BusinessLocation(id, employmentId, placeId, code, active)
private fun BusinessLocation.toEntity() = BusinessLocationEntity(id, employmentId, placeId, code, active)
private fun RouteEntity.toDomain() = Route(id, fromPlaceId, toPlaceId, distanceMeters, durationSeconds, RouteSource.valueOf(source), effectiveFrom, effectiveUntil)
private fun Route.toEntity() = RouteEntity(id, fromPlaceId, toPlaceId, distanceMeters, durationSeconds, source.name, effectiveFrom, effectiveUntil)
private fun BusinessTripEntity.toDomain() = BusinessTrip(id, date, employmentId, purpose, TransportMode.valueOf(transportMode), createdAt)
private fun BusinessTrip.toEntity() = BusinessTripEntity(id, date, employmentId, purpose, transportMode.name, createdAt)
private fun BusinessTripLegEntity.toDomain() = BusinessTripLeg(id, businessTripId, sequence, fromPlaceId, toPlaceId, fromAddress, toAddress, distanceMeters, RouteSource.valueOf(distanceSource), TransportMode.valueOf(transportMode))
private fun BusinessTripLeg.toEntity() = BusinessTripLegEntity(id, businessTripId, sequence, fromPlaceId, toPlaceId, fromAddress, toAddress, distanceMeters, distanceSource.name, transportMode.name)

class RoomEmploymentRepository(private val dao: EmploymentDao) : EmploymentRepository { override fun observeAll() = dao.observeAll().map { it.map(EmploymentEntity::toDomain) }; override suspend fun upsert(value: Employment) = dao.upsert(value.toEntity()) }
class RoomPlaceRepository(private val dao: PlaceDao) : PlaceRepository { override fun observeAll() = dao.observeAll().map { it.map(PlaceEntity::toDomain) }; override suspend fun upsert(value: Place) = dao.upsert(value.toEntity()) }
class RoomVehicleRepository(private val dao: VehicleDao) : VehicleRepository { override fun observeAll() = dao.observeAll().map { it.map(VehicleEntity::toDomain) }; override suspend fun upsert(value: Vehicle) = dao.upsert(value.toEntity()) }
class RoomEmploymentWorkplaceRepository(private val dao: EmploymentWorkplaceDao) : EmploymentWorkplaceRepository { override suspend fun upsert(value: EmploymentWorkplace) = dao.upsert(EmploymentWorkplaceEntity(value.employmentId, value.placeId, value.validFrom, value.validUntil)) }
class RoomCommuteProfileRepository(private val dao: CommuteProfileDao) : CommuteProfileRepository { override fun observeAll(): Flow<List<CommuteProfile>> = dao.observeAll().map { it.map(CommuteProfileEntity::toDomain) }; override suspend fun upsert(value: CommuteProfile) = dao.upsert(value.toEntity()) }
class RoomCommuteRecordRepository(private val dao: CommuteRecordDao) : CommuteRecordRepository { override fun observeAll(): Flow<List<CommuteRecord>> = dao.observeAll().map { it.map(CommuteRecordEntity::toDomain) }; override suspend fun upsert(value: CommuteRecord) = dao.upsert(value.toEntity()); override suspend fun delete(id: EntityId) = dao.delete(id) }
class RoomBusinessLocationRepository(private val dao: BusinessLocationDao) : BusinessLocationRepository { override fun observeAll(): Flow<List<BusinessLocation>> = dao.observeAll().map { it.map(BusinessLocationEntity::toDomain) }; override suspend fun upsert(value: BusinessLocation) = dao.upsert(value.toEntity()) }
class RoomRouteRepository(private val dao: RouteDao) : RouteRepository { override fun observeAll(): Flow<List<Route>> = dao.observeAll().map { it.map(RouteEntity::toDomain) }; override suspend fun upsert(value: Route) = dao.upsert(value.toEntity()) }
class RoomBusinessTripRepository(
    private val database: MatkahelpotinDatabase,
    private val dao: BusinessTripDao,
    private val legDao: BusinessTripLegDao,
) : BusinessTripRepository {
    override fun observeAll(): Flow<List<BusinessTrip>> = dao.observeAll().map { it.map(BusinessTripEntity::toDomain) }
    override suspend fun upsert(value: BusinessTrip) = dao.upsert(value.toEntity())
    override suspend fun delete(id: EntityId) = dao.delete(id)

    override suspend fun createWithLegs(trip: BusinessTrip, legs: List<BusinessTripLeg>) {
        require(legs.isNotEmpty()) { "A business trip must contain at least one leg" }
        require(legs.all { it.businessTripId == trip.id }) { "All legs must belong to the trip" }
        database.withTransaction {
            dao.upsert(trip.toEntity())
            legs.forEach { legDao.upsert(it.toEntity()) }
        }
    }

    override suspend fun deleteWithLegs(id: EntityId) {
        database.withTransaction {
            legDao.deleteForTrip(id)
            dao.delete(id)
        }
    }
}
class RoomBusinessTripLegRepository(private val dao: BusinessTripLegDao) : BusinessTripLegRepository { override fun observeAll(): Flow<List<BusinessTripLeg>> = dao.observeAll().map { it.map(BusinessTripLegEntity::toDomain) }; override suspend fun upsert(value: BusinessTripLeg) = dao.upsert(value.toEntity()); override suspend fun deleteForTrip(tripId: EntityId) = dao.deleteForTrip(tripId) }
