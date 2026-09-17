package fi.italeino.matkahelpotin

import android.app.Application
import androidx.room.Room
import fi.italeino.matkahelpotin.data.RoomEmploymentRepository
import fi.italeino.matkahelpotin.data.RoomPlaceRepository
import fi.italeino.matkahelpotin.data.RoomVehicleRepository
import fi.italeino.matkahelpotin.data.local.MatkahelpotinDatabase

class MatkahelpotinApplication : Application() {
    val database: MatkahelpotinDatabase by lazy {
        Room.databaseBuilder(this, MatkahelpotinDatabase::class.java, "matkahelpotin.db").build()
    }
    val employmentRepository by lazy { RoomEmploymentRepository(database.employmentDao()) }
    val placeRepository by lazy { RoomPlaceRepository(database.placeDao()) }
    val vehicleRepository by lazy { RoomVehicleRepository(database.vehicleDao()) }
}
