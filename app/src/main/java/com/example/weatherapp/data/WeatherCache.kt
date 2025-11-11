package com.example.weatherapp.data

import androidx.compose.runtime.mutableStateMapOf
import com.example.weatherapp.model.CachedWeather
import org.json.JSONArray
import org.json.JSONObject

object WeatherCache {
    val cache = mutableStateMapOf<String, CachedWeather>()

    fun isRecent(lat: String, lon: String, thresholdMinutes: Int = 30): Boolean {
        val key = "$lat,$lon"
        val last = cache[key]?.lastFetched ?: return false
        val elapsed = (System.currentTimeMillis() - last) / 1000 / 60
        return elapsed < thresholdMinutes
    }

    fun get(lat: String, lon: String): CachedWeather? = cache["$lat,$lon"]

    fun put(lat: String, lon: String, data: CachedWeather) {
        cache["$lat,$lon"] = data
    }

    fun toJson(): String {
        val json = JSONObject()
        cache.forEach { (key, value) ->
            val obj = JSONObject()
            obj.put("lastFetched", value.lastFetched)
            obj.put("hourlyData", JSONArray(value.hourlyData.map { JSONArray(listOf(it.first, it.second, it.third)) }))
            obj.put("weeklyData", JSONArray(value.weeklyData.map { JSONArray(listOf(it.first, it.second)) }))
            json.put(key, obj)
        }
        return json.toString()
    }

    fun fromJson(jsonStr: String) {
        cache.clear()
        val json = JSONObject(jsonStr)
        json.keys().forEach { key ->
            val obj = json.getJSONObject(key)
            val hourlyArray = obj.getJSONArray("hourlyData")
            val weeklyArray = obj.getJSONArray("weeklyData")
            val hourlyList = mutableListOf<Triple<String, Double, Double>>()
            val weeklyList = mutableListOf<Pair<String, Double>>()
            for (i in 0 until hourlyArray.length()) {
                val arr = hourlyArray.getJSONArray(i)
                hourlyList.add(Triple(arr.getString(0), arr.getDouble(1), arr.getDouble(2)))
            }
            for (i in 0 until weeklyArray.length()) {
                val arr = weeklyArray.getJSONArray(i)
                weeklyList.add(Pair(arr.getString(0), arr.getDouble(1)))
            }
            cache[key] = CachedWeather(hourlyList, weeklyList, obj.getLong("lastFetched"))
        }
    }
}
