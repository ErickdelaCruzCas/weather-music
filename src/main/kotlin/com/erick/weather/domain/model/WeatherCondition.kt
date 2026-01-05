package com.erick.weather.domain.model

import java.time.Instant

/**
 * Domain model representing weather conditions for a region.
 * This is the domain's representation, independent of external API models.
 */
data class WeatherCondition(
    val region: String,
    val condition: WeatherType,
    val temperature: Double,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val timestamp: Instant
)

enum class WeatherType {
    CLEAR,
    CLOUDS,
    RAIN,
    DRIZZLE,
    THUNDERSTORM,
    SNOW,
    MIST,
    FOG;

    companion object {
        fun fromString(value: String): WeatherType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: CLOUDS // Default fallback
        }
    }
}

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}
