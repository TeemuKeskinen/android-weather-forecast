package com.example.weatherapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.weatherapp.data.DataStoreManager
import com.example.weatherapp.data.WeatherCache
import com.example.weatherapp.model.LocationSelectionManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherAppWithDrawer(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val savedLocations = remember { mutableStateListOf<Triple<String, String, String>>() }
    var defaultLocation by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    // Load persisted data
    LaunchedEffect(Unit) {
        val locJson = DataStoreManager.loadLocations(context)
        val locArray = JSONArray(locJson)
        val defaultJson = DataStoreManager.loadDefaultLocation(context)

        if (locArray.length() == 0 || defaultJson.isEmpty()) {
            val tampere = Triple("Tampere", "61.4981", "23.7608")
            val examples = listOf(
                Triple("Helsinki", "60.1699", "24.9384"),
                Triple("Oulu", "65.0121", "25.4651"),
                Triple("Tokyo", "35.68", "139.76"),
                Triple("New York City", "40.75", "-73.97")
            )
            savedLocations.clear()
            savedLocations.add(tampere)
            savedLocations.addAll(examples)
            val jsonArray = JSONArray()
            (listOf(tampere) + examples).forEach {
                val obj = JSONObject()
                obj.put("name", it.first)
                obj.put("lat", it.second)
                obj.put("lon", it.third)
                jsonArray.put(obj)
            }
            DataStoreManager.saveLocations(context, jsonArray.toString())

            // Set default location to Tampere
            defaultLocation = tampere
            val obj = JSONObject()
            obj.put("name", tampere.first)
            obj.put("lat", tampere.second)
            obj.put("lon", tampere.third)
            DataStoreManager.saveDefaultLocation(context, obj.toString())
            LocationSelectionManager.selectedName = tampere.first
            LocationSelectionManager.selectedLat = tampere.second
            LocationSelectionManager.selectedLon = tampere.third
        } else {
            // Load saved locations from DataStore
            savedLocations.clear()
            for (i in 0 until locArray.length()) {
                val obj = locArray.getJSONObject(i)
                savedLocations.add(Triple(obj.getString("name"), obj.getString("lat"), obj.getString("lon")))
            }
            // Load default location from DataStore
            val obj = JSONObject(defaultJson)
            val triple = Triple(obj.getString("name"), obj.getString("lat"), obj.getString("lon"))
            defaultLocation = triple
            LocationSelectionManager.selectedName = triple.first
            LocationSelectionManager.selectedLat = triple.second
            LocationSelectionManager.selectedLon = triple.third
        }

        val cacheJson = DataStoreManager.loadCache(context)
        WeatherCache.fromJson(cacheJson)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                ) {
                    Text(
                        "Saved Locations",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    savedLocations.forEachIndexed { index, (name, lat, lon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        LocationSelectionManager.selectedName = name
                                        LocationSelectionManager.selectedLat = lat
                                        LocationSelectionManager.selectedLon = lon
                                        coroutineScope.launch { drawerState.close() }
                                    }
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                if (defaultLocation?.first == name) {
                                    Text(
                                        "Default",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        val obj = JSONObject()
                                        obj.put("name", name)
                                        obj.put("lat", lat)
                                        obj.put("lon", lon)
                                        DataStoreManager.saveDefaultLocation(
                                            context,
                                            obj.toString()
                                        )
                                        defaultLocation = Triple(name, lat, lon)
                                        Toast.makeText(
                                            context,
                                            "$name set as default location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Set Default")
                                }
                                IconButton(onClick = {
                                    locationToDelete = savedLocations[index]
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        WeatherSearchScreen(
            openDrawer = { coroutineScope.launch { drawerState.open() } },
            addLocation = { name, lat, lon ->
                savedLocations.add(Triple(name, lat, lon))
                coroutineScope.launch {
                    val jsonArray = JSONArray()
                    savedLocations.forEach {
                        val obj = JSONObject()
                        obj.put("name", it.first)
                        obj.put("lat", it.second)
                        obj.put("lon", it.third)
                        jsonArray.put(obj)
                    }
                    DataStoreManager.saveLocations(context, jsonArray.toString())
                }
            },
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme
        ) }


    //  Delete dialog
    if (showDeleteDialog && locationToDelete != null) {
        val (name, _, _) = locationToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Location") },
            text = { Text("Are you sure you want to delete $name?") },
            confirmButton = {
                TextButton(onClick = {
                    savedLocations.remove(locationToDelete)
                    coroutineScope.launch {
                        val jsonArray = JSONArray()
                        savedLocations.forEach {
                            val obj = JSONObject()
                            obj.put("name", it.first)
                            obj.put("lat", it.second)
                            obj.put("lon", it.third)
                            jsonArray.put(obj)
                        }
                        DataStoreManager.saveLocations(context, jsonArray.toString())
                    }
                    showDeleteDialog = false
                    locationToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    locationToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}