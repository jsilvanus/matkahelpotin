package fi.italeino.matkahelpotin.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportManifest(
    val format: String,
    val formatVersion: Int,
    val schemaVersion: Int,
    val applicationVersion: String,
    val createdAt: String,
    val dateRange: ExportDateRange,
    val datasets: List<String>,
)

@Serializable
data class ExportDateRange(val from: String, val until: String)

@Serializable
data class Checksums(val algorithm: String, val files: Map<String, String>)

@Serializable
data class ExportEnvelope<T>(
    val schemaVersion: Int,
    val dateRange: ExportDateRange,
    val records: List<T>,
)
