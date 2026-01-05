package com.erick.weather.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Application DTO for weather responses.
 * Maps to OpenAPI schema: WeatherResponse
 */
data class WeatherResponse(
    val region: String,
    val weather: Weather
)

/**
 * Application DTO for weather details.
 * Maps to OpenAPI schema: Weather
 */
data class Weather(
    val condition: String,
    val temperature: Double,
    val temperatureUnit: String = "celsius",
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val timestamp: Instant
)
