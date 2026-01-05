package com.erick.weather.infrastructure.client.weather

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * External DTOs for OpenWeather API responses.
 * These models represent the structure of data returned by OpenWeather API.
 * DO NOT expose these models outside the infrastructure layer.
 */

data class OpenWeatherResponse(
    val coord: Coordinates?,
    val weather: List<WeatherInfo>,
    val main: MainWeatherData,
    val wind: Wind?,
    val clouds: Clouds?,
    val dt: Long,
    val name: String
)

data class Coordinates(
    val lon: Double,
    val lat: Double
)

data class WeatherInfo(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class MainWeatherData(
    val temp: Double,
    val humidity: Int,
    @JsonProperty("feels_like")
    val feelsLike: Double?,
    @JsonProperty("temp_min")
    val tempMin: Double?,
    @JsonProperty("temp_max")
    val tempMax: Double?,
    val pressure: Int?
)

data class Wind(
    val speed: Double,
    val deg: Int?
)

data class Clouds(
    val all: Int
)

data class GeocodingResponse(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)
