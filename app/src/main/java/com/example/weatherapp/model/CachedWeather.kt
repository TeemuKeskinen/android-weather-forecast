package com.example.weatherapp.model

data class CachedWeather(
    val hourlyData: List<Triple<String, Double, Double>>,
    val weeklyData: List<Pair<String, Double>>,
    val lastFetched: Long
)
