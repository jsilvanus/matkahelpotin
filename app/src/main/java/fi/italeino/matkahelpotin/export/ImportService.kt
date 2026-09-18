package fi.italeino.matkahelpotin.export

import androidx.room.withTransaction
import fi.italeino.matkahelpotin.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipInputStream

enum class ImportAction { ADD, DESTROY, NONE }

data class ImportPlan(
    val commute: Map<LocalDate, ImportAction>,
    val businessTrips: Map<LocalDate, ImportAction>,
)

data class ImportResult(
    val commuteImported: Int,
    val businessTripsImported: Int,
    val commuteDestroyed: Int,
    val businessTripsDestroyed: Int,
)

class ImportService(private val database: MatkahelpotinDatabase) {
    private val json = Json { ignoreUnknownKeys = false }

    suspend fun inspect(zipBytes: ByteArray): ImportPackage {
        ExportValidator.validateZip(zipBytes)
        val files = readFiles(zipBytes)
        val manifest = json.decodeFromString<ExportManifest>(files.getValue("manifest.json"))
        require(manifest.format == "matkahelpotin-travel-export") { "Unsupported export format" }
        require(manifest.formatVersion == 1) { "Unsupported export format version" }
        require(manifest.schemaVersion == 1) { "Unsupported export schema version" }

        val from = LocalDate.parse(manifest.dateRange.from)
        val until = LocalDate.parse(manifest.dateRange.until)
        val datasets = manifest.datasets.map {
            when (it) {
                "commute" -> ExportDataset.COMMUTE
                "business-trips" -> ExportDataset.BUSINESS_TRIPS
                else -> error("Unsupported dataset: $it")
            }
        }.toSet()

        val commute = files["commute.json"]?.let(::parseCommute) ?: emptyList()
        val business = files["business-trips.json"]?.let(::parseBusiness) ?: emptyList()
        val existingCommuteDates = database.commuteRecordDao().observeAll().first().map { it.date }.toSet()
        val existingBusinessDates = database.businessTripDao().observeAll().first().map { it.date }.toSet()

        require(commute.all { it.date in from..until }) { "Commute record outside manifest date range" }
        require(business.all { it.trip.date in from..until }) { "Business trip outside manifest date range" }

        return ImportPackage(
            from, until, datasets, commute, business,
            existingCommuteDates intersect commute.map { it.date }.toSet(),
            existingBusinessDates intersect business.map { it.trip.date }.toSet(),
        )
    }

    suspend fun import(packageData: ImportPackage, plan: ImportPlan): ImportResult =
        database.withTransaction {
            var ci = 0
            var bi = 0
            var cd = 0
            var bd = 0

            if (ExportDataset.COMMUTE in packageData.datasets) {
                plan.commute.filterValues { it == ImportAction.DESTROY }.keys.forEach { date ->
                    cd += database.commuteRecordDao().deleteByDate(date)
                }
                packageData.commute.filter { plan.commute[it.date] == ImportAction.ADD || plan.commute[it.date] == ImportAction.DESTROY }
                    .forEach { database.commuteRecordDao().upsert(it.record); ci++ }
                packageData.commute.map { it.profile }.distinctBy { it.id }.forEach(database.commuteProfileDao()::upsert)
                packageData.commute.map { it.employment }.distinctBy { it.id }.forEach(database.employmentDao()::upsert)
                packageData.commute.flatMap { it.places }.distinctBy { it.id }.forEach(database.placeDao()::upsert)
            }

            if (ExportDataset.BUSINESS_TRIPS in packageData.datasets) {
                plan.businessTrips.filterValues { it == ImportAction.DESTROY }.keys.forEach { date ->
                    val trips = database.businessTripDao().observeAll().first().filter { it.date == date }
                    trips.forEach {
                        database.businessTripLegDao().deleteForTrip(it.id)
                        database.businessTripDao().delete(it.id)
                    }
                    bd += trips.size
                }
                packageData.business.filter {
                    plan.businessTrips[it.trip.date] == ImportAction.ADD || plan.businessTrips[it.trip.date] == ImportAction.DESTROY
                }.forEach {
                    database.businessTripDao().upsert(it.trip)
                    it.legs.forEach(database.businessTripLegDao()::upsert)
                    bi++
                }
                packageData.business.flatMap { it.employments }.distinctBy { it.id }.forEach(database.employmentDao()::upsert)
                packageData.business.flatMap { it.places }.distinctBy { it.id }.forEach(database.placeDao()::upsert)
                packageData.business.flatMap { it.locations }.distinctBy { it.id }.forEach(database.businessLocationDao()::upsert)
                packageData.business.flatMap { it.routes }.distinctBy { it.id }.forEach(database.routeDao()::upsert)
            }
            ImportResult(ci, bi, cd, bd)
        }

    data class ImportPackage(
        val from: LocalDate, val until: LocalDate, val datasets: Set<ExportDataset>,
        val commute: List<CommuteItem>, val business: List<BusinessItem>,
        val existingCommuteDates: Set<LocalDate>, val existingBusinessTripDates: Set<LocalDate>,
    )
    data class CommuteItem(val record: CommuteRecordEntity, val profile: CommuteProfileEntity, val employment: EmploymentEntity, val places: List<PlaceEntity>, val date: LocalDate)
    data class BusinessItem(val trip: BusinessTripEntity, val legs: List<BusinessTripLegEntity>, val employments: List<EmploymentEntity>, val places: List<PlaceEntity>, val locations: List<BusinessLocationEntity>, val routes: List<RouteEntity>)

    private fun parseCommute(raw: String): List<CommuteItem> {
        val root = json.parseToJsonElement(raw).jsonObject
        val employments = root["employments"]!!.jsonArray.map(::employment)
        val places = root["places"]!!.jsonArray.map(::place)
        val profiles = root["profiles"]!!.jsonArray.map(::profile)
        val profileMap = profiles.associateBy { it.id }
        val placeMap = places.associateBy { it.id }
        return root["records"]!!.jsonArray.map {
            val record = commuteRecord(it.jsonObject)
            val profile = profileMap[record.commuteProfileId] ?: error("Commute record references missing profile")
            val employment = employments.firstOrNull { e -> e.id == profile.employmentId } ?: error("Profile references missing employment")
            require(placeMap.containsKey(profile.homePlaceId) && placeMap.containsKey(profile.workplaceId)) { "Profile references missing place" }
            CommuteItem(record, profile, employment, listOfNotNull(placeMap[profile.homePlaceId], placeMap[profile.workplaceId]).distinctBy { p -> p.id }, record.date)
        }
    }

    private fun parseBusiness(raw: String): List<BusinessItem> {
        val root = json.parseToJsonElement(raw).jsonObject
        val employments = root["employments"]!!.jsonArray.map(::employment)
        val places = root["places"]!!.jsonArray.map(::place)
        val locations = root["businessLocations"]!!.jsonArray.map(::businessLocation)
        val routes = root["routes"]!!.jsonArray.map(::route)
        val trips = root["trips"]!!.jsonArray.map(::businessTrip)
        val legs = root["legs"]!!.jsonArray.map(::leg).groupBy { it.businessTripId }
        val employmentMap = employments.associateBy { it.id }
        val placeMap = places.associateBy { it.id }
        return trips.map { trip ->
            val tripLegs = legs[trip.id] ?: error("Business trip has no legs")
            require(tripLegs.map { it.sequence }.distinct().size == tripLegs.size) { "Duplicate business-trip leg sequence" }
            require(employmentMap.containsKey(trip.employmentId)) { "Trip references missing employment" }
            tripLegs.forEach { l ->
                if (l.fromPlaceId != null) require(placeMap.containsKey(l.fromPlaceId))
                if (l.toPlaceId != null) require(placeMap.containsKey(l.toPlaceId))
                require(l.distanceMeters >= 0)
            }
            BusinessItem(trip, tripLegs.sortedBy { it.sequence }, employments, places, locations, routes)
        }
    }

    private fun employment(o: JsonObject) = EmploymentEntity(UUID.fromString(o.req("id")), o.req("employerName"), o.opt("employerIdentifier"), o.opt("description"), o.opt("activeFrom")?.let(LocalDate::parse), o.opt("activeUntil")?.let(LocalDate::parse), Instant.parse(o.req("createdAt")), Instant.parse(o.req("updatedAt")))
    private fun place(o: JsonObject) = PlaceEntity(UUID.fromString(o.req("id")), o.req("name"), o.req("type"), o.opt("address"), o.opt("postalCode"), o.opt("city"), o.req("country"), o.optDouble("latitude"), o.optDouble("longitude"), o.opt("source"), Instant.parse(o.req("createdAt")), Instant.parse(o.req("updatedAt")))
    private fun profile(o: JsonObject) = CommuteProfileEntity(UUID.fromString(o.req("id")), UUID.fromString(o.req("employmentId")), UUID.fromString(o.req("homePlaceId")), UUID.fromString(o.req("workplaceId")), o.req("transportMode"), o.optLong("distanceMeters"), o.optLong("ticketPriceCents"), o.reqInt("tripsPerDay"), o.opt("activeFrom")?.let(LocalDate::parse), o.opt("activeUntil")?.let(LocalDate::parse), o.reqLong("colourArgb"), o.reqBool("enabled"))
    private fun commuteRecord(o: JsonObject) = CommuteRecordEntity(UUID.fromString(o.req("id")), LocalDate.parse(o.req("date")), UUID.fromString(o.req("commuteProfileId")), o.reqInt("tripCount"), o.optLong("distanceMetersSnapshot"), o.optLong("costCentsSnapshot"), Instant.parse(o.req("createdAt")))
    private fun businessLocation(o: JsonObject) = BusinessLocationEntity(UUID.fromString(o.req("id")), UUID.fromString(o.req("employmentId")), UUID.fromString(o.req("placeId")), o.opt("code"), o.reqBool("active"))
    private fun route(o: JsonObject) = RouteEntity(UUID.fromString(o.req("id")), UUID.fromString(o.req("fromPlaceId")), UUID.fromString(o.req("toPlaceId")), o.reqLong("distanceMeters"), o.optLong("durationSeconds"), o.req("source"), o.opt("effectiveFrom")?.let(LocalDate::parse), o.opt("effectiveUntil")?.let(LocalDate::parse))
    private fun businessTrip(o: JsonObject) = BusinessTripEntity(UUID.fromString(o.req("id")), LocalDate.parse(o.req("date")), UUID.fromString(o.req("employmentId")), o.opt("purpose"), o.req("transportMode"), o.opt("reimbursementRateIdSnapshot")?.let(UUID::fromString), o.optLong("reimbursementRateCentsPerKmSnapshot"), o.opt("mileagePolicyIdSnapshot")?.let(UUID::fromString), o.optLong("mileageRateCentsPerKmSnapshot"), o.optLong("mileageLimitMetersSnapshot"), Instant.parse(o.req("createdAt")))
    private fun leg(o: JsonObject) = BusinessTripLegEntity(UUID.fromString(o.req("id")), UUID.fromString(o.req("businessTripId")), o.reqInt("sequence"), o.opt("fromPlaceId")?.let(UUID::fromString), o.opt("toPlaceId")?.let(UUID::fromString), o.opt("fromAddress"), o.opt("toAddress"), o.reqLong("distanceMeters"), o.req("distanceSource"), o.optLong("calculatedDistanceMeters"), o.opt("calculatedDistanceProvider"), o.optLong("manualDistanceOverrideMeters"), o.req("transportMode"))

    private fun readFiles(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip -> while (true) { val e = zip.nextEntry ?: break; result[e.name] = zip.readBytes().toString(Charsets.UTF_8) } }
        return result
    }
    private fun JsonObject.req(name: String) = this[name]?.jsonPrimitive?.content ?: error("Missing $name")
    private fun JsonObject.reqInt(name: String) = this[name]?.jsonPrimitive?.int ?: error("Missing $name")
    private fun JsonObject.reqLong(name: String) = this[name]?.jsonPrimitive?.long ?: error("Missing $name")
    private fun JsonObject.reqBool(name: String) = this[name]?.jsonPrimitive?.boolean ?: error("Missing $name")
    private fun JsonObject.opt(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.optLong(name: String): Long? = this[name]?.jsonPrimitive?.longOrNull
    private fun JsonObject.optDouble(name: String): Double? = this[name]?.jsonPrimitive?.doubleOrNull
}
