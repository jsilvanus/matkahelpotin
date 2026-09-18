package fi.italeino.matkahelpotin

import android.app.Application
import androidx.room.Room
import fi.italeino.matkahelpotin.data.*
import fi.italeino.matkahelpotin.data.local.MatkahelpotinDatabase
import fi.italeino.matkahelpotin.data.local.MIGRATION_1_2
import fi.italeino.matkahelpotin.data.local.MIGRATION_2_3
import fi.italeino.matkahelpotin.data.local.MIGRATION_3_4

class MatkahelpotinApplication : Application() {
    val database: MatkahelpotinDatabase by lazy { Room.databaseBuilder(this, MatkahelpotinDatabase::class.java, "matkahelpotin.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build() }
    val employmentRepository by lazy { RoomEmploymentRepository(database.employmentDao()) }
    val placeRepository by lazy { RoomPlaceRepository(database.placeDao()) }
    val vehicleRepository by lazy { RoomVehicleRepository(database.vehicleDao()) }
    val commuteProfileRepository by lazy { RoomCommuteProfileRepository(database.commuteProfileDao()) }
    val commuteRecordRepository by lazy { RoomCommuteRecordRepository(database.commuteRecordDao()) }
    val businessLocationRepository by lazy { RoomBusinessLocationRepository(database.businessLocationDao()) }
    val routeRepository by lazy { RoomRouteRepository(database.routeDao()) }
    val businessTripRepository by lazy { RoomBusinessTripRepository(database, database.businessTripDao(), database.businessTripLegDao()) }
    val businessTripLegRepository by lazy { RoomBusinessTripLegRepository(database.businessTripLegDao()) }
    val reimbursementRateRepository by lazy { RoomReimbursementRateRepository(database.reimbursementRateDao()) }
    val mileagePolicyRepository by lazy { RoomMileagePolicyRepository(database.mileagePolicyDao()) }
}

