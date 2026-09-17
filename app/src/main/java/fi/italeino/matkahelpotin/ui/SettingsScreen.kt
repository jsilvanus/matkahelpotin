package fi.italeino.matkahelpotin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.italeino.matkahelpotin.domain.PlaceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val employments by viewModel.employments.collectAsState()
    val places by viewModel.places.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    var employer by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Matkahelpotin") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Initial setup", style = MaterialTheme.typography.headlineSmall) }
            item { Text("Everything is stored locally on this device.") }
            item {
                OutlinedTextField(employer, { employer = it }, label = { Text("Employer") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.addEmployment(employer); employer = "" }) { Text("Add employment") }
            }
            item {
                OutlinedTextField(place, { place = it }, label = { Text("Place name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.addPlace(place, PlaceType.HOME); place = "" }) { Text("Add home") }
                    OutlinedButton(onClick = { viewModel.addPlace(place, PlaceType.WORKPLACE); place = "" }) { Text("Add workplace") }
                }
            }
            item {
                OutlinedTextField(vehicle, { vehicle = it }, label = { Text("Vehicle") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.addVehicle(vehicle, null); vehicle = "" }) { Text("Add vehicle") }
            }
            item { HorizontalDivider() }
            item { Text("Employments", style = MaterialTheme.typography.titleMedium) }
            items(employments) { Text("• ${it.employerName}") }
            item { Text("Places", style = MaterialTheme.typography.titleMedium) }
            items(places) { Text("• ${it.name} — ${it.type.name.lowercase()}") }
            item { Text("Vehicles", style = MaterialTheme.typography.titleMedium) }
            items(vehicles) { Text("• ${it.name}${it.registration?.let { r -> " ($r)" } ?: ""}") }
        }
    }
}
