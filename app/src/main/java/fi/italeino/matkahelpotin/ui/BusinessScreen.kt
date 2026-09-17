package fi.italeino.matkahelpotin.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.italeino.matkahelpotin.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields

private enum class BusinessMode { WEEK, HISTORY, SETUP }

@Composable
fun BusinessScreen(viewModel: BusinessViewModel) {
    val employments by viewModel.employments.collectAsState(emptyList())
    val places by viewModel.places.collectAsState(emptyList())
    val routes by viewModel.routes.collectAsState(emptyList())
    val trips by viewModel.trips.collectAsState(emptyList())
    val legs by viewModel.legs.collectAsState(emptyList())
    val locations by viewModel.businessLocations.collectAsState(emptyList())
    var weekStart by remember { mutableStateOf(LocalDate.now().with(WeekFields.ISO.dayOfWeek(), 1)) }
    var employmentId by remember { mutableStateOf<EntityId?>(null) }
    var mode by remember { mutableStateOf(BusinessMode.WEEK) }

    LaunchedEffect(employments) { if (employmentId == null) employmentId = employments.firstOrNull()?.id }
    val selectedEmployment = employments.firstOrNull { it.id == employmentId }
    val placeNames = places.associate { it.id to it.name }
    val allowed = locations.filter { it.employmentId == employmentId && it.active }.map { it.placeId }.toSet()
    val visibleRoutes = routes.filter { allowed.isEmpty() || (it.fromPlaceId in allowed && it.toPlaceId in allowed) }

    Scaffold(topBar = { TopAppBar(title = { Text("Business trips") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (employments.isEmpty()) {
                Text("Add an employment in Settings before recording business trips.")
                return@Column
            }
            EmploymentSelector(employments, selectedEmployment) { employmentId = it }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { mode = BusinessMode.WEEK }) { Text("Week") }
                TextButton(onClick = { mode = BusinessMode.HISTORY }) { Text("History") }
                TextButton(onClick = { mode = BusinessMode.SETUP }) { Text("Setup") }
            }
            when (mode) {
                BusinessMode.WEEK -> {
                    WeekView(viewModel, weekStart, visibleRoutes, trips, legs, placeNames, employmentId!!, routes)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { weekStart = weekStart.minusWeeks(1) }) { Text("Previous week") }
                        OutlinedButton(onClick = { weekStart = weekStart.plusWeeks(1) }) { Text("Next week") }
                    }
                    val weeklyMeters = legs.filter { leg ->
                        trips.any { it.id == leg.businessTripId && it.date >= weekStart && it.date < weekStart.plusDays(7) && it.employmentId == employmentId }
                    }.sumOf { it.distanceMeters }
                    Text("Week total: ${"%.1f".format(weeklyMeters / 1000.0)} km")
                }
                BusinessMode.HISTORY -> BusinessHistory(trips, legs, placeNames, employmentId!!)
                BusinessMode.SETUP -> BusinessSetup(viewModel, selectedEmployment!!, places, locations, routes, placeNames)
            }
        }
    }
}

@Composable
private fun EmploymentSelector(employments: List<Employment>, selected: Employment?, onSelect: (EntityId) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selected?.employerName ?: "Select employment") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            employments.forEach { employment ->
                DropdownMenuItem(text = { Text(employment.employerName) }, onClick = { onSelect(employment.id); expanded = false })
            }
        }
    }
}

@Composable
private fun WeekView(viewModel: BusinessViewModel, weekStart: LocalDate, routes: List<Route>, trips: List<BusinessTrip>, legs: List<BusinessTripLeg>, placeNames: Map<EntityId, String>, employmentId: EntityId, allRoutes: List<Route>) {
    if (routes.isEmpty()) {
        Text("No predefined business routes yet. Open Setup to add locations and routes.")
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0..6).forEach { offset ->
            val date = weekStart.plusDays(offset.toLong())
            Column(Modifier.width(170.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${date.dayOfWeek.shortName()}\n${date.dayOfMonth}.${date.monthValue}.", style = MaterialTheme.typography.titleSmall)
                routes.forEach { route ->
                    val matching = trips.filter { it.date == date && it.employmentId == employmentId }.filter { trip ->
                        val first = legs.filter { it.businessTripId == trip.id }.minByOrNull { it.sequence }
                        first?.fromPlaceId == route.fromPlaceId && first.toPlaceId == route.toPlaceId
                    }
                    val oneWayCount = matching.count { trip -> legs.count { it.businessTripId == trip.id } == 1 }
                    val returnCount = matching.count { trip -> legs.count { it.businessTripId == trip.id } == 2 }
                    val reverse = allRoutes.firstOrNull { it.fromPlaceId == route.toPlaceId && it.toPlaceId == route.fromPlaceId }
                    Card {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${placeNames[route.fromPlaceId] ?: "?"} → ${placeNames[route.toPlaceId] ?: "?"}")
                            Text("${route.distanceMeters / 1000.0} km one way", style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                TextButton(onClick = { viewModel.removeOne(date, route, false) }) { Text("−") }
                                Text("$oneWayCount")
                                TextButton(onClick = { viewModel.addOneWay(date, employmentId, route) }) { Text("+") }
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                TextButton(onClick = { viewModel.removeOne(date, route, true) }) { Text("−") }
                                Text("return $returnCount")
                                TextButton(enabled = reverse != null, onClick = { reverse?.let { viewModel.addReturn(date, employmentId, route, it) } }) { Text("+") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessHistory(trips: List<BusinessTrip>, legs: List<BusinessTripLeg>, placeNames: Map<EntityId, String>, employmentId: EntityId) {
    val rows = trips.filter { it.employmentId == employmentId }.sortedByDescending { it.date }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Business-trip history", style = MaterialTheme.typography.titleMedium)
        if (rows.isEmpty()) Text("No business trips recorded yet.")
        rows.forEach { trip ->
            val tripLegs = legs.filter { it.businessTripId == trip.id }.sortedBy { it.sequence }
            val distance = tripLegs.sumOf { it.distanceMeters } / 1000.0
            val path = tripLegs.joinToString(" → ") { leg ->
                placeNames[leg.fromPlaceId] ?: leg.fromAddress ?: "?"
            } + (tripLegs.lastOrNull()?.let { " → ${placeNames[it.toPlaceId] ?: it.toAddress ?: "?"}" } ?: "")
            ListItem(headlineContent = { Text(trip.date.toString()) }, supportingContent = { Text("$path · ${"%.1f".format(distance)} km") })
            HorizontalDivider()
        }
    }
}

@Composable
private fun BusinessSetup(viewModel: BusinessViewModel, employment: Employment, places: List<Place>, locations: List<BusinessLocation>, routes: List<Route>, placeNames: Map<EntityId, String>) {
    var routeFrom by remember { mutableStateOf<EntityId?>(null) }
    var routeTo by remember { mutableStateOf<EntityId?>(null) }
    var distance by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Business locations", style = MaterialTheme.typography.titleMedium)
        Text("Use the existing places as employer-defined locations.", style = MaterialTheme.typography.bodySmall)
        places.forEach { place ->
            val already = locations.any { it.employmentId == employment.id && it.placeId == place.id && it.active }
            if (!already) OutlinedButton(onClick = { viewModel.addBusinessLocation(employment.id, place.id, place.name) }) { Text("Add ${place.name}") }
        }
        val businessPlaces = locations.filter { it.employmentId == employment.id && it.active }.mapNotNull { l -> places.firstOrNull { it.id == l.placeId } }
        Text("Routes", style = MaterialTheme.typography.titleMedium)
        SimplePlaceSelector("From", businessPlaces, routeFrom) { routeFrom = it }
        SimplePlaceSelector("To", businessPlaces, routeTo) { routeTo = it }
        OutlinedTextField(distance, { distance = it }, label = { Text("One-way distance (km)") })
        Button(enabled = routeFrom != null && routeTo != null && distance.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true, onClick = { viewModel.addRoute(routeFrom!!, routeTo!!, distance); distance = "" }) { Text("Add employer-defined route") }
        routes.forEach { route -> Text("${placeNames[route.fromPlaceId]} → ${placeNames[route.toPlaceId]}: ${route.distanceMeters / 1000.0} km") }
    }
}

@Composable
private fun SimplePlaceSelector(label: String, places: List<Place>, selected: EntityId?, onSelect: (EntityId) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = places.firstOrNull { it.id == selected }?.name ?: "Select place"
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("$label: $name") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            places.forEach { place -> DropdownMenuItem(text = { Text(place.name) }, onClick = { onSelect(place.id); expanded = false }) }
        }
    }
}

private fun DayOfWeek.shortName(): String = name.take(3).lowercase().replaceFirstChar { it.uppercase() }
