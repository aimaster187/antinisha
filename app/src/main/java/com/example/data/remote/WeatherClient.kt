package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WeatherSnapshot(
    val temperatureC: Double,
    val windKmh: Double,
    val conditionLabel: String,
    val locationLabel: String
)

class WeatherClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
        locationLabel: String
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code,wind_speed_10m"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) error("Weather HTTP ${res.code}")
                val body = res.body?.string().orEmpty()
                val current = JSONObject(body).getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val wind = current.getDouble("wind_speed_10m")
                val code = current.getInt("weather_code")
                WeatherSnapshot(
                    temperatureC = temp,
                    windKmh = wind,
                    conditionLabel = decodeWmoCode(code),
                    locationLabel = locationLabel
                )
            }
        }
    }

    private fun decodeWmoCode(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Fair"
    }
}
