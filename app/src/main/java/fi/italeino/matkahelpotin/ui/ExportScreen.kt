package fi.italeino.matkahelpotin.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.italeino.matkahelpotin.export.ExportViewModel
import java.time.LocalDate

@Composable
fun ExportScreen(viewModel: ExportViewModel) {
    var from by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var until by remember { mutableStateOf(LocalDate.now().toString()) }
    var commute by remember { mutableStateOf(true) }
    var business by remember { mutableStateOf(true) }
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && state.data != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(state.data) }
            viewModel.clear()
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Export", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(from, { from = it }, label = { Text("From (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(until, { until = it }, label = { Text("Until (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(commute, { commute = !commute }, label = { Text("Commute") })
            FilterChip(business, { business = !business }, label = { Text("Business trips") })
        }
        Button(enabled = !state.busy, onClick = {
            runCatching { LocalDate.parse(from) }.onSuccess { start ->
                runCatching { LocalDate.parse(until) }.onSuccess { end ->
                    viewModel.export(start, end, commute, business, "0.1.0")
                }
            }
        }) { Text(if (state.busy) "Building…" else "Build export") }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.data != null) {
            Button(onClick = { launcher.launch("travel-export.zip") }) { Text("Save travel-export.zip") }
        }
        Text("The export is generated locally. No data is uploaded.")
    }
}
