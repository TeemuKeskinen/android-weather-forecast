package com.example.weatherapp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object LocationSelectionManager {
    var selectedName: String? by mutableStateOf(null)
    var selectedLat: String? by mutableStateOf(null)
    var selectedLon: String? by mutableStateOf(null)
}
