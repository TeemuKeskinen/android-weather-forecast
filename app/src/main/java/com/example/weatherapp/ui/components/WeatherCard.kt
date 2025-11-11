package com.example.weatherapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WeatherCard(time: String, temperature: Double, rain: Double? = null) {
    Card(modifier = Modifier.width(100.dp).height(120.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(time)
            Text("🌡 %.1f°C".format(temperature))
            if (rain != null) Text("🌧 %.1f mm".format(rain))
        }
    }
}
