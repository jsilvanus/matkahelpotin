package fi.italeino.matkahelpotin.export

import fi.italeino.matkahelpotin.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.zip.ZipInputStream
import java.io.ByteArrayInputStream

class ExportServiceTest {
    private val emptyEmployment = object : EmploymentRepository {
        override fun observeAll(): Flow<List<Employment>> = flowOf(emptyList())
        override suspend fun upsert(employment: Employment) = Unit
    }
    private val emptyPlace = object : PlaceRepository {
        override fun observeAll(): Flow<List<Place>> = flowOf(emptyList())
        override suspend fun upsert(place: Place) = Unit
    }
    private val emptyProfile = object : CommuteProfileRepository {
        override fun observeAll(): Flow<List<CommuteProfile>> = flowOf(emptyList())
        override suspend fun upsert(profile: CommuteProfile) = Unit
    }
    private val emptyRecord = object : CommuteRecordRepository {
        override fun observeAll(): Flow<List<CommuteRecord>> = flowOf(emptyList())
        override suspend fun upsert(record: CommuteRecord) = Unit
        override suspend fun delete(id: EntityId) = Unit
    }
    private val emptyBusinessLocation = object : BusinessLocationRepository {
        override fun observeAll(): Flow<List<BusinessLocation>> = flowOf(emptyList())
        override suspend fun upsert(location: BusinessLocation) = Unit
    }
    private val emptyRoute = object : RouteRepository {
        override fun observeAll(): Flow<List<Route>> = flowOf(emptyList())
        override suspend fun upsert(route: Route) = Unit
    }
    private val emptyTrip = object : BusinessTripRepository {
        override fun observeAll(): Flow<List<BusinessTrip>> = flowOf(emptyList())
        override suspend fun upsert(trip: BusinessTrip) = Unit
        override suspend fun delete(id: EntityId) = Unit
        override suspend fun createWithLegs(trip: BusinessTrip, legs: List<BusinessTripLeg>) = Unit
        override suspend fun deleteWithLegs(id: EntityId) = Unit
    }
    private val emptyLeg = object : BusinessTripLegRepository {
        override fun observeAll(): Flow<List<BusinessTripLeg>> = flowOf(emptyList())
        override suspend fun upsert(leg: BusinessTripLeg) = Unit
        override suspend fun deleteForTrip(tripId: EntityId) = Unit
    }

    private fun service() = ExportService(
        emptyEmployment, emptyPlace, emptyProfile, emptyRecord,
        emptyBusinessLocation, emptyRoute, emptyTrip, emptyLeg,
    )

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDateRange() {
        ExportSelection(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 18), setOf(ExportDataset.COMMUTE))
    }

    @Test
    fun createsCommuteOnlyPackageWithChecksums() = kotlinx.coroutines.test.runTest {
        val bytes = service().buildZip(
            ExportSelection(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 18), setOf(ExportDataset.COMMUTE)),
            "0.1.0",
        )
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names += entry.name
            }
        }
        assertEquals(listOf("checksums.json", "commute.json", "manifest.json"), names)
    }

    @Test
    fun createsBusinessOnlyPackageWithoutCommute() = kotlinx.coroutines.test.runTest {
        val bytes = service().buildZip(
            ExportSelection(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 18), setOf(ExportDataset.BUSINESS_TRIPS)),
            "0.1.0",
        )
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names += entry.name
            }
        }
        assertTrue("business-trips.json" in names)
        assertTrue("commute.json" !in names)
        assertTrue("checksums.json" in names)
    }
}
