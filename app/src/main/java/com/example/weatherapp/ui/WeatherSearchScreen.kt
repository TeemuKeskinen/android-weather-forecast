package com.example.weatherapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.weatherapp.data.DataStoreManager
import com.example.weatherapp.data.WeatherCache
import com.example.weatherapp.data.network.WeatherApiClient
import com.example.weatherapp.model.CachedWeather
import com.example.weatherapp.model.LocationSelectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherSearchScreen(
    openDrawer: () -> Unit,
    addLocation: (String, String, String) -> Unit,
    darkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val example = WeatherApiClient()
    val coroutineScope = rememberCoroutineScope()

    var latitude by remember { mutableStateOf("61.4981") }
    var longitude by remember { mutableStateOf("23.7608") }
    var locationName by remember { mutableStateOf("Tampere") }

    var hourlyData by remember { mutableStateOf<List<Triple<String, Double, Double>>>(emptyList()) }
    var weeklyData by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newLat by remember { mutableStateOf("") }
    var newLon by remember { mutableStateOf("") }
    var coordError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    suspend fun fetchWeather(lat: String, lon: String): Boolean {
        hourlyData = emptyList()
        weeklyData = emptyList()

        return try {
            if (WeatherCache.isRecent(lat, lon)) {
                WeatherCache.get(lat, lon)?.let {
                    hourlyData = it.hourlyData
                    weeklyData = it.weeklyData
                    return true
                }
            }

            val url =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&hourly=temperature_2m,rain&current=temperature_2m&timezone=auto"
            val response = withContext(Dispatchers.IO) { example.run(url) }

            val json = JSONObject(response)
            val hourly = json.getJSONObject("hourly")
            val timeArray = hourly.getJSONArray("time")
            val tempArray = hourly.getJSONArray("temperature_2m")
            val rainArray = hourly.getJSONArray("rain")

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val hourlyList = mutableListOf<Triple<String, Double, Double>>()
            val weeklyList = mutableListOf<Pair<String, Double>>()

            for (i in 0 until timeArray.length()) {
                val dateTimeStr = timeArray.getString(i)
                val temp = tempArray.getDouble(i)
                val rain = rainArray.getDouble(i)
                val dateTime = LocalDateTime.parse(dateTimeStr, formatter)

                if (hourlyList.size < 24) {
                    hourlyList.add(Triple(dateTime.toLocalTime().toString(), temp, rain))
                }
                if (dateTime.hour == 15) {
                    weeklyList.add(Pair(dateTime.toLocalDate().toString(), temp))
                    if (weeklyList.size == 7) break
                }
            }

            hourlyData = hourlyList
            weeklyData = weeklyList

            WeatherCache.put(lat, lon, CachedWeather(hourlyList, weeklyList, System.currentTimeMillis()))
            DataStoreManager.saveCache(context, WeatherCache.toJson())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isValidCoordinate(lat: String, lon: String): Boolean {
        return try {
            val latNum = lat.toDouble()
            val lonNum = lon.toDouble()
            latNum in -90.0..90.0 && lonNum in -180.0..180.0
        } catch (e: NumberFormatException) {
            false
        }
    }

    // Load initial data
    LaunchedEffect(
        LocationSelectionManager.selectedLat,
        LocationSelectionManager.selectedLon,
        LocationSelectionManager.selectedName
    ) {
        LocationSelectionManager.selectedLat?.let { lat ->
            LocationSelectionManager.selectedLon?.let { lon ->
                latitude = lat
                longitude = lon
                locationName = LocationSelectionManager.selectedName ?: "Unknown"
                fetchWeather(lat, lon)
            }
        }
    }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                delay(1500)
                fetchWeather(latitude, longitude)
                delay(300)
                isRefreshing = false
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🌤 Weather App") },
                    navigationIcon = {
                        IconButton(onClick = openDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = !showAddDialog }) {
                    Icon(
                        imageVector = if (showAddDialog) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (showAddDialog) "Close" else "Add"
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Currently viewing: $locationName ($latitude, $longitude)",
                    style = MaterialTheme.typography.titleMedium
                )

                if (hourlyData.isNotEmpty()) {
                    Text(
                        "Today's Hourly Forecast (${LocalDate.now()}):",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hourlyData) { (time, temp, rain) ->
                            Card(modifier = Modifier.width(100.dp).height(120.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(time)
                                    Text("🌡 %.1f°C".format(temp))
                                    Text("🌧 %.1f mm".format(rain))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "No cached or live weather data available for this location yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (weeklyData.isNotEmpty()) {
                    Text("Next 7 Days (15:00):", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(weeklyData) { (date, temp) ->
                            Card(modifier = Modifier.width(120.dp).height(100.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(date)
                                    Text("🌡 %.1f°C".format(temp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings dialog
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Settings") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dark theme")
                        Switch(checked = darkTheme, onCheckedChange = { checked -> onToggleTheme(checked) })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) { Text("Close") }
                }
            )
        }

        // Add location dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Location") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = {
                                newName = it
                                nameError = false
                            },
                            label = { Text("Location Name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = newLat,
                            onValueChange = {
                                newLat = it
                                coordError = false
                            },
                            label = { Text("Latitude (-90 to 90)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newLon,
                            onValueChange = {
                                newLon = it
                                coordError = false
                            },
                            label = { Text("Longitude (-180 to 180)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (nameError) {
                            Text(
                                "Missing location name! Please add a name to save.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (coordError) {
                            Text(
                                "Invalid coordinates! Please enter latitude between -90 and 90, and longitude between -180 and 180 (e.g., 61.4981, 23.7608).",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isBlank()) {
                            nameError = true
                            return@TextButton
                        }
                        if (!isValidCoordinate(newLat, newLon)) {
                            coordError = true
                            return@TextButton
                        }

                        addLocation(newName, newLat, newLon)
                        locationName = newName
                        latitude = newLat
                        longitude = newLon

                        coroutineScope.launch {
                            val success = fetchWeather(newLat, newLon)
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Location added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Location added — data will load when online.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }

                        showAddDialog = false
                        newName = ""
                        newLat = ""
                        newLon = ""
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        coordError = false
                        nameError = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
