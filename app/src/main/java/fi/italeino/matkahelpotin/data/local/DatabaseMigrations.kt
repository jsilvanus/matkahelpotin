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


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE business_trip ADD COLUMN reimbursementRateIdSnapshot TEXT")
        database.execSQL("ALTER TABLE business_trip ADD COLUMN reimbursementRateCentsPerKmSnapshot INTEGER")
        database.execSQL("ALTER TABLE business_trip ADD COLUMN mileagePolicyIdSnapshot TEXT")
        database.execSQL("ALTER TABLE business_trip ADD COLUMN mileageRateCentsPerKmSnapshot INTEGER")
        database.execSQL("ALTER TABLE business_trip ADD COLUMN mileageLimitMetersSnapshot INTEGER")
    }
}


val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE mileage_policy ADD COLUMN validFrom TEXT NOT NULL DEFAULT '2000-01-01'")
        database.execSQL("ALTER TABLE mileage_policy ADD COLUMN validUntil TEXT")
        database.execSQL("UPDATE mileage_policy SET validFrom = printf('%04d-01-01', year)")
    }
}
