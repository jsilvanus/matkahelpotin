package fi.italeino.matkahelpotin.export

import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

object ExportValidator {
    fun validateZip(bytes: ByteArray) {
        val files = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory) { "Export must not contain directories" }
                require(entry.name in setOf("manifest.json", "commute.json", "business-trips.json", "checksums.json")) { "Unexpected export file: ${entry.name}" }
                require(entry.name !in files) { "Duplicate export file: ${entry.name}" }
                files[entry.name] = zip.readBytes()
            }
        }
        require("manifest.json" in files) { "Missing manifest.json" }
        require("checksums.json" in files) { "Missing checksums.json" }

        val json = Json { ignoreUnknownKeys = false }
        val manifest = json.parseToJsonElement(files.getValue("manifest.json").decodeToString()).jsonObject
        require(manifest["format"]?.jsonPrimitive?.content == "matkahelpotin-travel-export") { "Unsupported export format" }
        require(manifest["formatVersion"]?.jsonPrimitive?.int == 1) { "Unsupported export format version" }
        val datasets = manifest["datasets"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet()
            ?: error("Manifest datasets are missing")
        require(datasets.isNotEmpty()) { "Manifest must contain at least one dataset" }
        require(("commute" in datasets) == ("commute.json" in files)) { "Commute dataset does not match archive contents" }
        require(("business-trips" in datasets) == ("business-trips.json" in files)) { "Business dataset does not match archive contents" }

        files.filterKeys { it != "checksums.json" }.forEach { (name, data) ->
            val root = json.parseToJsonElement(data.decodeToString()).jsonObject
            require(root["schemaVersion"]?.jsonPrimitive?.int == 1) { "${name} has unsupported schema version" }
            require(root["dateRange"]?.jsonObject?.containsKey("from") == true) { "${name} has no date range" }
            require(root["dateRange"]?.jsonObject?.containsKey("until") == true) { "${name} has no date range" }
        }

        val checksums = json.parseToJsonElement(files.getValue("checksums.json").decodeToString()).jsonObject
        require(checksums["algorithm"]?.jsonPrimitive?.content == "SHA-256") { "Unsupported checksum algorithm" }
        val declared = checksums["files"]?.jsonObject ?: error("Checksum file list is missing")
        require(declared.keys == files.keys - "checksums.json") { "Checksum file list does not match archive contents" }
        declared.forEach { (name, value) ->
            val expected = sha256(files.getValue(name))
            require(value.jsonPrimitive.content == expected) { "Checksum mismatch: $name" }
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
