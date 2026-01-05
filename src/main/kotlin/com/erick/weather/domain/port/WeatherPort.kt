package com.erick.weather.domain.port

import com.erick.weather.domain.model.WeatherCondition
import reactor.core.publisher.Mono

/**
 * Port (interface) for weather data access.
 * Implementation will be in infrastructure layer (WeatherClient adapter).
 */
interface WeatherPort {

    /**
     * Get current weather for a city
     */
    fun getWeatherByCity(city: String): Mono<WeatherCondition>

    /**
     * Get current weather by coordinates
     */
    fun getWeatherByCoordinates(lat: Double, lon: Double): Mono<WeatherCondition>

    /**
     * Resolve city name to coordinates
     */
    fun resolveCoordinates(city: String): Mono<Pair<Double, Double>>

    /**
     * Health check
     */
    fun isHealthy(): Mono<Boolean>
}
