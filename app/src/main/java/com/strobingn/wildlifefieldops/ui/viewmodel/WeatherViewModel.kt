package com.strobingn.wildlifefieldops.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class WeatherUiState(
    val loading: Boolean = true,
    val locationLabel: String = "Hudson Valley",
    val temperatureF: Int? = null,
    val apparentTemperatureF: Int? = null,
    val highF: Int? = null,
    val lowF: Int? = null,
    val precipitationChance: Int? = null,
    val windMph: Int? = null,
    val condition: String = "Loading weather…",
    val error: String? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(WeatherUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading && _state.value.temperatureF != null) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val location = lastKnownCoordinates()
            val result = runCatching { fetchWeather(location.first, location.second) }
            _state.value = result.getOrElse {
                WeatherUiState(
                    loading = false,
                    locationLabel = location.third,
                    condition = "Weather unavailable",
                    error = it.message ?: "Unable to load weather"
                )
            }
        }
    }

    private suspend fun lastKnownCoordinates(): Triple<Double, Double, String> = withContext(Dispatchers.IO) {
        val fallback = Triple(41.4348, -74.0400, "Cornwall, NY")
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return@withContext fallback

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best = manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return@withContext fallback
        Triple(best.latitude, best.longitude, "Current location")
    }

    private suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherUiState = withContext(Dispatchers.IO) {
        val endpoint = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=1"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) error("Weather service returned ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            WeatherUiState(
                loading = false,
                locationLabel = if (latitude == 41.4348 && longitude == -74.0400) "Cornwall, NY" else "Current location",
                temperatureF = current.optDouble("temperature_2m").takeIf { !it.isNaN() }?.toInt(),
                apparentTemperatureF = current.optDouble("apparent_temperature").takeIf { !it.isNaN() }?.toInt(),
                highF = daily.getJSONArray("temperature_2m_max").optDouble(0).takeIf { !it.isNaN() }?.toInt(),
                lowF = daily.getJSONArray("temperature_2m_min").optDouble(0).takeIf { !it.isNaN() }?.toInt(),
                precipitationChance = daily.getJSONArray("precipitation_probability_max").optInt(0),
                windMph = current.optDouble("wind_speed_10m").takeIf { !it.isNaN() }?.toInt(),
                condition = weatherLabel(current.optInt("weather_code", -1))
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun weatherLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95, 96, 99 -> "Thunderstorms"
        else -> "Current conditions"
    }
}
