package fi.italeino.matkahelpotin.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun uuidToString(value: java.util.UUID?): String? = value?.toString()
    @TypeConverter fun stringToUuid(value: String?): java.util.UUID? = value?.let(java.util.UUID::fromString)
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun instantToString(value: Instant?): String? = value?.toString()
    @TypeConverter fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)
}

@Entity(tableName = "employment")
data class EmploymentEntity(@PrimaryKey val id: java.util.UUID, val employerName: String, val employerIdentifier: String?, val description: String?, val activeFrom: LocalDate?, val activeUntil: LocalDate?, val createdAt: Instant, val updatedAt: Instant)
@Entity(tableName = "place")
data class PlaceEntity(@PrimaryKey val id: java.util.UUID, val name: String, val type: String, val address: String?, val postalCode: String?, val city: String?, val country: String, val latitude: Double?, val longitude: Double?, val source: String?, val createdAt: Instant, val updatedAt: Instant)
@Entity(primaryKeys = ["employmentId", "placeId"], tableName = "employment_workplace")
data class EmploymentWorkplaceEntity(val employmentId: java.util.UUID, val placeId: java.util.UUID, val validFrom: LocalDate?, val validUntil: LocalDate?)
@Entity(tableName = "commute_profile")
data class CommuteProfileEntity(@PrimaryKey val id: java.util.UUID, val employmentId: java.util.UUID, val homePlaceId: java.util.UUID, val workplaceId: java.util.UUID, val transportMode: String, val distanceMeters: Long?, val ticketPriceCents: Long?, val tripsPerDay: Int, val activeFrom: LocalDate?, val activeUntil: LocalDate?, val colourArgb: Long, val enabled: Boolean)
@Entity(tableName = "commute_record")
data class CommuteRecordEntity(@PrimaryKey val id: java.util.UUID, val date: LocalDate, val commuteProfileId: java.util.UUID, val tripCount: Int, val distanceMetersSnapshot: Long?, val costCentsSnapshot: Long?, val createdAt: Instant)
@Entity(tableName = "business_location")
data class BusinessLocationEntity(@PrimaryKey val id: java.util.UUID, val employmentId: java.util.UUID, val placeId: java.util.UUID, val code: String?, val active: Boolean)
@Entity(tableName = "route")
data class RouteEntity(@PrimaryKey val id: java.util.UUID, val fromPlaceId: java.util.UUID, val toPlaceId: java.util.UUID, val distanceMeters: Long, val durationSeconds: Long?, val source: String, val effectiveFrom: LocalDate?, val effectiveUntil: LocalDate?)
@Entity(tableName = "business_trip")
data class BusinessTripEntity(@PrimaryKey val id: java.util.UUID, val date: LocalDate, val employmentId: java.util.UUID, val purpose: String?, val transportMode: String, val createdAt: Instant)
@Entity(tableName = "business_trip_leg")
data class BusinessTripLegEntity(@PrimaryKey val id: java.util.UUID, val businessTripId: java.util.UUID, val sequence: Int, val fromPlaceId: java.util.UUID?, val toPlaceId: java.util.UUID?, val fromAddress: String?, val toAddress: String?, val distanceMeters: Long, val distanceSource: String, val transportMode: String)
@Entity(tableName = "vehicle")
data class VehicleEntity(@PrimaryKey val id: java.util.UUID, val name: String, val registration: String?, val active: Boolean)
@Entity(tableName = "reimbursement_rate")
data class ReimbursementRateEntity(@PrimaryKey val id: java.util.UUID, val type: String, val amountCents: Long, val currency: String, val unit: String, val validFrom: LocalDate, val validUntil: LocalDate?, val jurisdiction: String)
@Entity(tableName = "mileage_policy")
data class MileagePolicyEntity(@PrimaryKey val id: java.util.UUID, val year: Int, val mileageLimitMeters: Long?, val mileageRateCentsPerKm: Long, val jurisdiction: String)

@Dao interface EmploymentDao { @Query("SELECT * FROM employment ORDER BY employerName") fun observeAll(): Flow<List<EmploymentEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: EmploymentEntity) }
@Dao interface PlaceDao { @Query("SELECT * FROM place ORDER BY name") fun observeAll(): Flow<List<PlaceEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: PlaceEntity) }
@Dao interface VehicleDao { @Query("SELECT * FROM vehicle ORDER BY name") fun observeAll(): Flow<List<VehicleEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: VehicleEntity) }
@Dao interface EmploymentWorkplaceDao { @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: EmploymentWorkplaceEntity) }
@Dao interface CommuteProfileDao { @Query("SELECT * FROM commute_profile ORDER BY id") fun observeAll(): Flow<List<CommuteProfileEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: CommuteProfileEntity) }
@Dao interface CommuteRecordDao { @Query("SELECT * FROM commute_record ORDER BY date") fun observeAll(): Flow<List<CommuteRecordEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: CommuteRecordEntity); @Query("DELETE FROM commute_record WHERE id = :id") suspend fun delete(id: java.util.UUID) }
@Dao interface BusinessLocationDao { @Query("SELECT * FROM business_location ORDER BY code") fun observeAll(): Flow<List<BusinessLocationEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: BusinessLocationEntity) }
@Dao interface RouteDao { @Query("SELECT * FROM route ORDER BY fromPlaceId, toPlaceId") fun observeAll(): Flow<List<RouteEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: RouteEntity) }
@Dao interface BusinessTripDao { @Query("SELECT * FROM business_trip ORDER BY date DESC") fun observeAll(): Flow<List<BusinessTripEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: BusinessTripEntity); @Query("DELETE FROM business_trip WHERE id = :id") suspend fun delete(id: java.util.UUID) }
@Dao interface BusinessTripLegDao { @Query("SELECT * FROM business_trip_leg ORDER BY businessTripId, sequence") fun observeAll(): Flow<List<BusinessTripLegEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: BusinessTripLegEntity); @Query("DELETE FROM business_trip_leg WHERE businessTripId = :tripId") suspend fun deleteForTrip(tripId: java.util.UUID) }
