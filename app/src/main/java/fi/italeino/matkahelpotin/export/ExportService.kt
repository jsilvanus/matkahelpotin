package fi.italeino.matkahelpotin.export

import fi.italeino.matkahelpotin.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportDataset { COMMUTE, BUSINESS_TRIPS }

data class ExportSelection(
    val from: LocalDate,
    val until: LocalDate,
    val datasets: Set<ExportDataset>,
) {
    init {
        require(!from.isAfter(until)) { "Export start date must not be after end date" }
        require(datasets.isNotEmpty()) { "At least one dataset must be selected" }
    }
}

class ExportService(
    private val employmentRepository: EmploymentRepository,
    private val placeRepository: PlaceRepository,
    private val commuteProfileRepository: CommuteProfileRepository,
    private val commuteRecordRepository: CommuteRecordRepository,
    private val businessLocationRepository: BusinessLocationRepository,
    private val routeRepository: RouteRepository,
    private val businessTripRepository: BusinessTripRepository,
    private val businessTripLegRepository: BusinessTripLegRepository,
) {
    suspend fun buildZip(selection: ExportSelection, applicationVersion: String): ByteArray {
        val files = linkedMapOf<String, String>()
        val range = ExportDateRange(selection.from.toString(), selection.until.toString())
        val employments = employmentRepository.observeAll().first()
        val places = placeRepository.observeAll().first()

        if (ExportDataset.COMMUTE in selection.datasets) {
            val profiles = commuteProfileRepository.observeAll().first()
            val records = commuteRecordRepository.observeAll().first().filter { it.date in selection.from..selection.until }
            val usedProfiles = profiles.filter { p -> records.any { it.commuteProfileId == p.id } }
            val usedEmploymentIds = usedProfiles.map { it.employmentId }.toSet()
            val usedPlaceIds = usedProfiles.flatMap { listOf(it.homePlaceId, it.workplaceId) }.toSet()
            files["commute.json"] = JsonObject(
                mapOf(
                    "schemaVersion" to JsonPrimitive(1),
                    "dateRange" to Json.encodeToJsonElement(range),
                    "employments" to JsonArray(employments.filter { it.id in usedEmploymentIds }.map(::employmentJson)),
                    "places" to JsonArray(places.filter { it.id in usedPlaceIds }.map(::placeJson)),
                    "profiles" to JsonArray(usedProfiles.map(::commuteProfileJson)),
                    "records" to JsonArray(records.map(::commuteRecordJson)),
                )
            ).toString()
        }

        if (ExportDataset.BUSINESS_TRIPS in selection.datasets) {
            val trips = businessTripRepository.observeAll().first().filter { it.date in selection.from..selection.until }
            val legs = businessTripLegRepository.observeAll().first().filter { it.businessTripId in trips.map { t -> t.id }.toSet() }
            val usedEmploymentIds = trips.map { it.employmentId }.toSet()
            val locations = businessLocationRepository.observeAll().first().filter { it.employmentId in usedEmploymentIds }
            val routes = routeRepository.observeAll().first()
            val usedPlaceIds = locations.map { it.placeId }.toSet()
            files["business-trips.json"] = JsonObject(
                mapOf(
                    "schemaVersion" to JsonPrimitive(1),
                    "dateRange" to Json.encodeToJsonElement(range),
                    "employments" to JsonArray(employments.filter { it.id in usedEmploymentIds }.map(::employmentJson)),
                    "places" to JsonArray(places.filter { it.id in usedPlaceIds }.map(::placeJson)),
                    "businessLocations" to JsonArray(locations.map(::businessLocationJson)),
                    "routes" to JsonArray(routes.filter { it.fromPlaceId in usedPlaceIds && it.toPlaceId in usedPlaceIds }.map(::routeJson)),
                    "trips" to JsonArray(trips.map(::businessTripJson)),
                    "legs" to JsonArray(legs.sortedWith(compareBy<BusinessTripLeg> { it.businessTripId.toString() }.thenBy { it.sequence }).map(::businessTripLegJson)),
                )
            ).toString()
        }

        val manifest = ExportManifest(
            format = "matkahelpotin-travel-export",
            formatVersion = 1,
            schemaVersion = 1,
            applicationVersion = applicationVersion,
            createdAt = Instant.now().toString(),
            dateRange = range,
            datasets = selection.datasets.map { if (it == ExportDataset.COMMUTE) "commute" else "business-trips" }.sorted(),
        )
        files["manifest.json"] = Json.encodeToString(manifest)
        val hashes = files.mapValues { sha256(it.value.toByteArray(Charsets.UTF_8)) }
        files["checksums.json"] = Json.encodeToString(Checksums("SHA-256", hashes))
        return zip(files).also(ExportValidator::validateZip)
    }

    private fun employmentJson(v: Employment) = buildJsonObject {
        put("id", v.id.toString()); put("employerName", v.employerName)
        put("employerIdentifier", v.employerIdentifier); put("description", v.description)
        put("activeFrom", v.activeFrom?.toString()); put("activeUntil", v.activeUntil?.toString())
        put("createdAt", v.createdAt.toString()); put("updatedAt", v.updatedAt.toString())
    }
    private fun placeJson(v: Place) = buildJsonObject {
        put("id", v.id.toString()); put("name", v.name); put("type", v.type.name)
        put("address", v.address); put("postalCode", v.postalCode); put("city", v.city)
        put("country", v.country); put("latitude", v.latitude); put("longitude", v.longitude); put("source", v.source)
        put("createdAt", v.createdAt.toString()); put("updatedAt", v.updatedAt.toString())
    }
    private fun commuteProfileJson(v: CommuteProfile) = buildJsonObject {
        put("id", v.id.toString()); put("employmentId", v.employmentId.toString()); put("homePlaceId", v.homePlaceId.toString())
        put("workplaceId", v.workplaceId.toString()); put("transportMode", v.transportMode.name)
        put("distanceMeters", v.distanceMeters); put("ticketPriceCents", v.ticketPriceCents); put("tripsPerDay", v.tripsPerDay)
        put("activeFrom", v.activeFrom?.toString()); put("activeUntil", v.activeUntil?.toString()); put("colourArgb", v.colourArgb); put("enabled", v.enabled)
    }
    private fun commuteRecordJson(v: CommuteRecord) = buildJsonObject {
        put("id", v.id.toString()); put("date", v.date.toString()); put("commuteProfileId", v.commuteProfileId.toString())
        put("tripCount", v.tripCount); put("distanceMetersSnapshot", v.distanceMetersSnapshot); put("costCentsSnapshot", v.costCentsSnapshot); put("createdAt", v.createdAt.toString())
    }
    private fun businessLocationJson(v: BusinessLocation) = buildJsonObject {
        put("id", v.id.toString()); put("employmentId", v.employmentId.toString()); put("placeId", v.placeId.toString()); put("code", v.code); put("active", v.active)
    }
    private fun routeJson(v: Route) = buildJsonObject {
        put("id", v.id.toString()); put("fromPlaceId", v.fromPlaceId.toString()); put("toPlaceId", v.toPlaceId.toString())
        put("distanceMeters", v.distanceMeters); put("durationSeconds", v.durationSeconds); put("source", v.source.name)
        put("effectiveFrom", v.effectiveFrom?.toString()); put("effectiveUntil", v.effectiveUntil?.toString())
    }
    private fun businessTripJson(v: BusinessTrip) = buildJsonObject {
        put("id", v.id.toString()); put("date", v.date.toString()); put("employmentId", v.employmentId.toString())
        put("purpose", v.purpose); put("transportMode", v.transportMode.name)
        put("reimbursementRateIdSnapshot", v.reimbursementRateIdSnapshot?.toString()); put("reimbursementRateCentsPerKmSnapshot", v.reimbursementRateCentsPerKmSnapshot)
        put("mileagePolicyIdSnapshot", v.mileagePolicyIdSnapshot?.toString()); put("mileageRateCentsPerKmSnapshot", v.mileageRateCentsPerKmSnapshot); put("mileageLimitMetersSnapshot", v.mileageLimitMetersSnapshot)
        put("createdAt", v.createdAt.toString())
    }
    private fun businessTripLegJson(v: BusinessTripLeg) = buildJsonObject {
        put("id", v.id.toString()); put("businessTripId", v.businessTripId.toString()); put("sequence", v.sequence)
        put("fromPlaceId", v.fromPlaceId?.toString()); put("toPlaceId", v.toPlaceId?.toString())
        put("fromAddress", v.fromAddress); put("toAddress", v.toAddress)
        put("distanceMeters", v.distanceMeters); put("distanceSource", v.distanceSource.name)
        put("calculatedDistanceMeters", v.calculatedDistanceMeters); put("calculatedDistanceProvider", v.calculatedDistanceProvider)
        put("manualDistanceOverrideMeters", v.manualDistanceOverrideMeters); put("transportMode", v.transportMode.name)
    }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun zip(files: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.toSortedMap().forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
