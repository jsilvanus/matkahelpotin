package fi.italeino.matkahelpotin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EmploymentEntity::class, PlaceEntity::class, EmploymentWorkplaceEntity::class,
        CommuteProfileEntity::class, CommuteRecordEntity::class, BusinessLocationEntity::class,
        RouteEntity::class, BusinessTripEntity::class, BusinessTripLegEntity::class,
        VehicleEntity::class, ReimbursementRateEntity::class, MileagePolicyEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MatkahelpotinDatabase : RoomDatabase() {
    abstract fun employmentDao(): EmploymentDao
    abstract fun placeDao(): PlaceDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun employmentWorkplaceDao(): EmploymentWorkplaceDao
    abstract fun commuteProfileDao(): CommuteProfileDao
    abstract fun commuteRecordDao(): CommuteRecordDao
    abstract fun businessLocationDao(): BusinessLocationDao
}
