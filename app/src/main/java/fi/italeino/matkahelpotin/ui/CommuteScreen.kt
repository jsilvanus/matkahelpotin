package fi.italeino.matkahelpotin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
    var infoDate by remember { mutableStateOf<LocalDate?>(null) }
    val days = (1..month.lengthOfMonth()).map(month::atDay)
    val leading = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val mileagePolicies by viewModel.mileagePolicies.collectAsState(emptyList())
    LaunchedEffect(employments) { if (employmentId == null) employmentId = employments.firstOrNull() }
    val visibleProfiles = profiles.filter { it.employmentId == employmentId }
    val visibleRecords = records.filter { record -> visibleProfiles.any { it.id == record.commuteProfileId } }
    val annualMileage = calculateAnnualCommuteMileageMeters(visibleRecords, visibleProfiles, month.year)
    val policy = selectMileagePolicy(mileagePolicies, month.atDay(1), scope = MileagePolicyScope.COMMUTE)
    val remaining = remainingMileageMeters(annualMileage, policy?.mileageLimitMeters)

    Scaffold(topBar = { TopAppBar(title = { Text("Commute") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (employments.size > 1) EmploymentDropdown(employments, employmentId) { employmentId = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", Modifier.padding(top = 12.dp))
                Button(onClick = { month = month.plusMonths(1) }) { Text("›") }
            }
            Text("Annual commute mileage: ${"%.1f".format(annualMileage / 1000.0)} km")
            policy?.mileageLimitMeters?.let { Text("Annual commute limit: ${"%.1f".format(it / 1000.0)} km; remaining: ${"%.1f".format((remaining ?: 0L) / 1000.0)} km") }
            if (profiles.isEmpty()) Text("Configure a commute profile in Settings first.")
            else {
                LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items((0 until leading).toList()) { Spacer(Modifier.padding(4.dp)) }
                    items(days) { date ->
                        val record = visibleRecords.firstOrNull { it.date == date }
                        val profile = record?.let { r -> visibleProfiles.firstOrNull { it.id == r.commuteProfileId } }
                        DayCell(date, profile, onClick = { viewModel.cycleDay(date, employmentId) }, onLongClick = { infoDate = date })
                    }
                }
            }
            Text("History", style = MaterialTheme.typography.titleMedium)
            visibleRecords.filter { it.date.year == month.year }.sortedByDescending { it.date }.forEach { record ->
                Text("${record.date}: ${record.tripCount} trips · ${"%.1f".format((record.distanceMetersSnapshot ?: 0L) / 1000.0)} km")
            }
        }
    }

    infoDate?.let { date ->
        val record = visibleRecords.firstOrNull { it.date == date }
        val profile = record?.let { r -> visibleProfiles.firstOrNull { it.id == r.commuteProfileId } }
        AlertDialog(
            onDismissRequest = { infoDate = null },
            title = { Text(date.toString()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (record == null) Text("No commute recorded.")
                    else {
                        Text("Trips: ${record.tripCount}")
                        Text("Distance: ${"%.1f".format((record.distanceMetersSnapshot ?: 0L) / 1000.0)} km")
                        profile?.let { Text("Transport: ${it.transportMode.name}") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { infoDate = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun EmploymentDropdown(employmentIds: List<EntityId>, selected: EntityId?, onSelect: (EntityId) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("Employment: " + (selected?.toString() ?: "Select")) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            employmentIds.forEach { id -> DropdownMenuItem(text = { Text(id.toString()) }, onClick = { onSelect(id); expanded = false }) }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, profile: CommuteProfile?, onClick: () -> Unit, onLongClick: () -> Unit) {
    val color = profile?.let { Color(it.colourArgb.toInt()) } ?: MaterialTheme.colorScheme.surfaceVariant
    Card(modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Column(Modifier.background(color).fillMaxWidth().padding(8.dp)) {
            Text("${date.dayOfMonth}")
            if (profile != null) Text(if (profile.transportMode.name == "PRIVATE_CAR") "car" else "PT")
        }
    }
}
