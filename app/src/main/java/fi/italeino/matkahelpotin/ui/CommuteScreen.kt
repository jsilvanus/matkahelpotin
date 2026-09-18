package fi.italeino.matkahelpotin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fi.italeino.matkahelpotin.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CommuteScreen(viewModel: CommuteViewModel) {
    val profiles by viewModel.profiles.collectAsState()
    var employmentId by remember { mutableStateOf<EntityId?>(null) }
    val records by viewModel.records.collectAsState()
    val employments = profiles.map { it.employmentId }.distinct()
    var month by remember { mutableStateOf(YearMonth.now()) }
    val days = (1..month.lengthOfMonth()).map(month::atDay)
    val leading = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val mileagePolicies by viewModel.mileagePolicies.collectAsState(emptyList())
    LaunchedEffect(employments) { if (employmentId == null) employmentId = employments.firstOrNull() }
    val visibleProfiles = profiles.filter { it.employmentId == employmentId }
    val visibleRecords = records.filter { record -> visibleProfiles.any { it.id == record.commuteProfileId } }
    val annualMileage = calculateAnnualCommuteMileageMeters(records, profiles, month.year, employmentId)
    val policy = selectMileagePolicy(mileagePolicies, month.atDay(1), scope = MileagePolicyScope.COMMUTE)
    val remaining = remainingMileageMeters(annualMileage, policy?.mileageLimitMeters)

    Scaffold(topBar = { TopAppBar(title = { Text("Commute") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (employments.size > 1) {
                DropdownField("Employment", profiles.firstOrNull { it.employmentId == employmentId }?.let { p -> "${p.employmentId}" } ?: "Select", employments.map { it to it.toString() }) { employmentId = it }
            }
            Text("History", style = MaterialTheme.typography.titleMedium)
            visibleRecords.filter { it.date.year == month.year }.sortedByDescending { it.date }.forEach { record ->
                val profile = visibleProfiles.firstOrNull { it.id == record.commuteProfileId }
                Text("${record.date}: ${record.tripCount} trips · ${"%.1f".format((record.distanceMetersSnapshot ?: 0L) / 1000.0)} km")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", Modifier.padding(top = 12.dp))
                Button(onClick = { month = month.plusMonths(1) }) { Text("›") }
            }
            Text("Annual commute mileage: ${"%.1f".format(annualMileage / 1000.0)} km")
            policy?.mileageLimitMeters?.let { Text("Annual commute limit: ${"%.1f".format(it / 1000.0)} km; remaining: ${"%.1f".format((remaining ?: 0L) / 1000.0)} km") }
            if (profiles.isEmpty()) {
                Text("Configure a commute profile in Settings first.")
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items((0 until leading).toList()) { Spacer(Modifier.padding(4.dp)) }
                    items(days) { date ->
                        val record = visibleRecords.firstOrNull { it.date == date }
                        val profile = record?.let { r -> visibleProfiles.firstOrNull { it.id == r.commuteProfileId } }
                        DayCell(date, profile) { viewModel.cycleDay(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, profile: CommuteProfile?, onClick: () -> Unit) {
    val color = profile?.let { Color(it.colourArgb.toInt()) } ?: MaterialTheme.colorScheme.surfaceVariant
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Column(Modifier.background(color).fillMaxWidth().padding(8.dp)) {
            Text("${date.dayOfMonth}")
            if (profile != null) Text(if (profile.transportMode.name == "PRIVATE_CAR") "car" else "PT")
        }
    }
}
