package fi.italeino.matkahelpotin.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.italeino.matkahelpotin.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ImportRoundTripTest {
    private val date = LocalDate.of(2026, 9, 18)

    @Test
    fun exportImportRoundTripPreservesCommuteAndBusinessData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = Room.inMemoryDatabaseBuilder(context, MatkahelpotinDatabase::class.java).build()
        val target = Room.inMemoryDatabaseBuilder(context, MatkahelpotinDatabase::class.java).build()

        try {
            val employmentId = UUID.randomUUID()
            val homeId = UUID.randomUUID()
            val workId = UUID.randomUUID()
            val profileId = UUID.randomUUID()
            val commuteId = UUID.randomUUID()
            val locationId = UUID.randomUUID()
            val routeId = UUID.randomUUID()
            val tripId = UUID.randomUUID()
            val legId = UUID.randomUUID()
            val now = Instant.parse("2026-09-18T06:00:00Z")

            source.employmentDao().upsert(
                EmploymentEntity(employmentId, "Test employer", null, null, null, null, now, now)
            )
            source.placeDao().upsert(PlaceEntity(homeId, "Home", "HOME", "Home street", null, "Pori", "FI", null, null, null, now, now))
            source.placeDao().upsert(PlaceEntity(workId, "Work", "WORKPLACE", "Work street", null, "Pori", "FI", null, null, null, now, now))
            source.commuteProfileDao().upsert(
                CommuteProfileEntity(profileId, employmentId, homeId, workId, "PRIVATE_CAR", 12000, null, 2, null, null, 0xffeeeeeeL, true)
            )
            source.commuteRecordDao().upsert(
                CommuteRecordEntity(commuteId, date, profileId, 2, 24000, null, now)
            )

            source.businessLocationDao().upsert(BusinessLocationEntity(locationId, employmentId, workId, "WORK", true))
            source.routeDao().upsert(RouteEntity(routeId, homeId, workId, 12000, 600, "EMPLOYER_DEFINED", null, null))
            source.businessTripDao().upsert(
                BusinessTripEntity(tripId, date, employmentId, "Meeting", "PRIVATE_CAR", null, null, null, null, null, now)
            )
            source.businessTripLegDao().upsert(
                BusinessTripLegEntity(legId, tripId, 0, homeId, workId, null, null, 12000, "EMPLOYER_DEFINED", null, null, null, "PRIVATE_CAR")
            )

            // Pre-existing records prove that DESTROY is scoped to the selected date.
            val oldCommuteId = UUID.randomUUID()
            source.commuteRecordDao().upsert(CommuteRecordEntity(oldCommuteId, date.minusDays(1), profileId, 1, 12000, null, now))
            target.employmentDao().upsert(
                EmploymentEntity(employmentId, "Test employer", null, null, null, null, now, now)
            )
            target.placeDao().upsert(PlaceEntity(homeId, "Home", "HOME", "Home street", null, "Pori", "FI", null, null, null, now, now))
            target.placeDao().upsert(PlaceEntity(workId, "Work", "WORKPLACE", "Work street", null, "Pori", "FI", null, null, null, now, now))
            target.commuteProfileDao().upsert(
                CommuteProfileEntity(profileId, employmentId, homeId, workId, "PRIVATE_CAR", 12000, null, 2, null, null, 0xffeeeeeeL, true)
            )
            target.commuteRecordDao().upsert(CommuteRecordEntity(oldCommuteId, date, profileId, 99, 999000, null, now))
            target.commuteRecordDao().upsert(CommuteRecordEntity(UUID.randomUUID(), date.minusDays(1), profileId, 7, 84000, null, now))
            val oldTripId = UUID.randomUUID()
            val oldLegId = UUID.randomUUID()
            target.businessLocationDao().upsert(BusinessLocationEntity(locationId, employmentId, workId, "WORK", true))
            target.routeDao().upsert(RouteEntity(routeId, homeId, workId, 12000, 600, "EMPLOYER_DEFINED", null, null))
            target.businessTripDao().upsert(BusinessTripEntity(oldTripId, date, employmentId, "Old meeting", "PRIVATE_CAR", null, null, null, null, null, now))
            target.businessTripLegDao().upsert(BusinessTripLegEntity(oldLegId, oldTripId, 0, homeId, workId, null, null, 12000, "EMPLOYER_DEFINED", null, null, null, "PRIVATE_CAR"))

            val service = ExportService(
                fi.italeino.matkahelpotin.data.RoomEmploymentRepository(source.employmentDao()),
                fi.italeino.matkahelpotin.data.RoomPlaceRepository(source.placeDao()),
                fi.italeino.matkahelpotin.data.RoomCommuteProfileRepository(source.commuteProfileDao()),
                fi.italeino.matkahelpotin.data.RoomCommuteRecordRepository(source.commuteRecordDao()),
                fi.italeino.matkahelpotin.data.RoomBusinessLocationRepository(source.businessLocationDao()),
                fi.italeino.matkahelpotin.data.RoomRouteRepository(source.routeDao()),
                fi.italeino.matkahelpotin.data.RoomBusinessTripRepository(source, source.businessTripDao(), source.businessTripLegDao()),
                fi.italeino.matkahelpotin.data.RoomBusinessTripLegRepository(source.businessTripLegDao()),
            )
            val zip = service.buildZip(ExportSelection(date, date, setOf(ExportDataset.COMMUTE, ExportDataset.BUSINESS_TRIPS)), "test")

            val importer = ImportService(target)
            val packageData = importer.inspect(zip)
            val result = importer.import(
                packageData,
                ImportPlan(
                    commute = mapOf(date to ImportAction.DESTROY),
                    businessTrips = mapOf(date to ImportAction.DESTROY),
                )
            )

            assertEquals(1, result.commuteImported)
            assertEquals(1, result.businessTripsImported)
            assertEquals(1, result.commuteDestroyed)
            assertEquals(1, result.businessTripsDestroyed)
            assertEquals(listOf(commuteId), target.commuteRecordDao().observeAll().first().map { it.id })
            assertEquals(listOf(tripId), target.businessTripDao().observeAll().first().map { it.id })
            assertEquals(listOf(legId), target.businessTripLegDao().observeAll().first().map { it.id })
            assertTrue(target.commuteProfileDao().observeAll().first().any { it.id == profileId })
        } finally {
            source.close()
            target.close()
        }
    }
}
