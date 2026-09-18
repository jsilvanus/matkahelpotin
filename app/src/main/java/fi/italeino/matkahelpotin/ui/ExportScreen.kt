package fi.italeino.matkahelpotin.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fi.italeino.matkahelpotin.BuildConfig
import fi.italeino.matkahelpotin.export.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ExportScreen(exportViewModel: ExportViewModel, importViewModel: ImportViewModel) {
    var from by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var until by remember { mutableStateOf(LocalDate.now().toString()) }
    var commute by remember { mutableStateOf(true) }
    var business by remember { mutableStateOf(true) }
    var importTab by remember { mutableStateOf(0) }
    val exportState by exportViewModel.state.collectAsState()
    val importState by importViewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && exportState.data != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(exportState.data) }
            exportViewModel.clear()
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) context.contentResolver.openInputStream(uri)?.use { importViewModel.inspect(it.readBytes()) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(importTab == 0, { importTab = 0 }, label = { Text("Export") })
            FilterChip(importTab == 1, { importTab = 1 }, label = { Text("Import") })
        }
        if (importTab == 0) {
            Text("Export", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(from, { from = it }, label = { Text("From (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(until, { until = it }, label = { Text("Until (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(commute, { commute = !commute }, label = { Text("Commute") })
                FilterChip(business, { business = !business }, label = { Text("Business trips") })
            }
            Button(enabled = !exportState.busy, onClick = {
                runCatching { LocalDate.parse(from) }.onSuccess { start ->
                    runCatching { LocalDate.parse(until) }.onSuccess { end ->
                        exportViewModel.export(start, end, commute, business, BuildConfig.VERSION_NAME)
                    }
                }
            }) { Text(if (exportState.busy) "Building…" else "Build export") }
            exportState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (exportState.data != null) Button(onClick = { saveLauncher.launch("travel-export.zip") }) { Text("Save travel-export.zip") }
            Text("The export is generated locally. No data is uploaded.")
        } else {
            Text("Import", style = MaterialTheme.typography.headlineSmall)
            Button(enabled = !importState.busy, onClick = { openLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) {
                Text("Choose travel-export.zip")
            }
            importState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            importState.result?.let { Text("Imported ${it.commuteImported} commute records and ${it.businessTripsImported} business trips.") }
            importState.packageData?.let { ImportCalendar(it, importViewModel) }
        }
    }
}

@Composable
private fun ImportCalendar(pkg: ImportService.ImportPackage, viewModel: ImportViewModel) {
    var dataset by remember { mutableStateOf(pkg.datasets.first()) }
    var month by remember { mutableStateOf(YearMonth.from(pkg.from)) }
    var decisions by remember { mutableStateOf<Map<Pair<ExportDataset, LocalDate>, ImportAction>>(emptyMap()) }
    var infoDate by remember { mutableStateOf<LocalDate?>(null) }

    val dates = if (dataset == ExportDataset.COMMUTE) pkg.commute.map { it.date }.toSet() else pkg.business.map { it.trip.date }.toSet()
    val existing = if (dataset == ExportDataset.COMMUTE) pkg.existingCommuteDates else pkg.existingBusinessTripDates

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pkg.datasets.forEach { d ->
                FilterChip(dataset == d, { dataset = d }, label = { Text(if (d == ExportDataset.COMMUTE) "Commute" else "Business trips") })
            }
        }
        Text("Tap a day to cycle: Add → Destroy → No import. Long-press for information.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
            Text(month.toString(), Modifier.padding(top = 10.dp))
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("🟢 Add"); Text("🔴 Destroy"); Text("⚪ No import")
        }
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(300.dp)) {
            items(leading) { Spacer(Modifier.size(4.dp)) }
            items(month.lengthOfMonth()) { index ->
                val date = month.atDay(index + 1)
                val key = dataset to date
                val action = decisions[key] ?: if (date in dates) ImportAction.ADD else ImportAction.NONE
                ImportDayCell(date, action, date in dates, date in existing,
                    onClick = { decisions = decisions + (key to nextAction(action)) },
                    onLongClick = { infoDate = date })
            }
        }
        Button(enabled = !viewModel.isBusy(), onClick = {
            val commutePlan = datesFor(pkg, ExportDataset.COMMUTE, decisions)
            val businessPlan = datesFor(pkg, ExportDataset.BUSINESS_TRIPS, decisions)
            viewModel.import(ImportPlan(commutePlan, businessPlan))
        }) { Text("Import selected dates") }

        infoDate?.let { date ->
            val imported = if (dataset == ExportDataset.COMMUTE) pkg.commute.filter { it.date == date } else pkg.business.filter { it.trip.date == date }
            AlertDialog(
                onDismissRequest = { infoDate = null },
                title = { Text(date.toString()) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (imported.isEmpty()) Text("No data in the import for this date.")
                        else if (dataset == ExportDataset.COMMUTE) pkg.commute.filter { it.date == date }.forEach {
                            Text("Commute: ${it.record.tripCount} trips · ${(it.record.distanceMetersSnapshot ?: 0L) / 1000.0} km")
                        } else pkg.business.filter { it.trip.date == date }.forEach {
                            Text("Business trip: ${it.trip.purpose ?: "No purpose"} · ${it.legs.size} legs · ${it.legs.sumOf { l -> l.distanceMeters } / 1000.0} km")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { infoDate = null }) { Text("Close") } },
            )
        }
    }
}

private fun datesFor(pkg: ImportService.ImportPackage, dataset: ExportDataset, decisions: Map<Pair<ExportDataset, LocalDate>, ImportAction>): Map<LocalDate, ImportAction> {
    val dates = if (dataset == ExportDataset.COMMUTE) pkg.commute.map { it.date }.toSet() else pkg.business.map { it.trip.date }.toSet()
    return dates.associateWith { decisions[dataset to it] ?: ImportAction.ADD }
}
private fun nextAction(action: ImportAction) = when (action) {
    ImportAction.ADD -> ImportAction.DESTROY
    ImportAction.DESTROY -> ImportAction.NONE
    ImportAction.NONE -> ImportAction.ADD
}
private fun ImportViewModel.isBusy(): Boolean = false

@Composable
private fun ImportDayCell(date: LocalDate, action: ImportAction, imported: Boolean, existing: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bg = when (action) {
        ImportAction.ADD -> Color(0xFFB7E4C7)
        ImportAction.DESTROY -> Color(0xFFFFB4AB)
        ImportAction.NONE -> MaterialTheme.colorScheme.surface
    }
    Card(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Column(Modifier.fillMaxWidth().background(bg).padding(7.dp)) {
            Text(date.dayOfMonth.toString())
            if (imported) Text(if (existing) "existing" else "data", style = MaterialTheme.typography.labelSmall)
        }
    }
}
