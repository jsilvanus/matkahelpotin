package fi.italeino.matkahelpotin.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE business_trip_leg ADD COLUMN calculatedDistanceMeters INTEGER")
        database.execSQL("ALTER TABLE business_trip_leg ADD COLUMN calculatedDistanceProvider TEXT")
        database.execSQL("ALTER TABLE business_trip_leg ADD COLUMN manualDistanceOverrideMeters INTEGER")
        database.execSQL(
            "UPDATE business_trip_leg SET distanceSource = 'ROUTING_PROVIDER', calculatedDistanceMeters = distanceMeters, calculatedDistanceProvider = 'legacy-map-provider' WHERE distanceSource = 'MAP_PROVIDER'"
        )
        database.execSQL(
            "UPDATE business_trip_leg SET distanceSource = 'MANUAL_OVERRIDE', manualDistanceOverrideMeters = distanceMeters WHERE distanceSource = 'MANUAL'"
        )
    }
}
