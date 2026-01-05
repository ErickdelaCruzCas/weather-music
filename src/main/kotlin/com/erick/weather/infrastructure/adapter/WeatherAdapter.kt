package com.erick.weather.infrastructure.adapter

import com.erick.weather.domain.exception.ExternalServiceErrorException
import com.erick.weather.domain.exception.ExternalServiceException
import com.erick.weather.domain.exception.RegionNotFoundException
import com.erick.weather.domain.model.TemperatureUnit
import com.erick.weather.domain.model.WeatherCondition
import com.erick.weather.domain.model.WeatherType
import com.erick.weather.domain.port.WeatherPort
import com.erick.weather.infrastructure.client.weather.WeatherClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Adapter that implements WeatherPort using WeatherClient.
 * Maps external DTOs to domain models.
 */
@Component
class WeatherAdapter(
    private val weatherClient: WeatherClient
) : WeatherPort {

    override fun getWeatherByCity(city: String): Mono<WeatherCondition> {
        return weatherClient.getWeatherByCity(city)
            .map { response ->
                WeatherCondition(
                    region = response.name,
                    condition = WeatherType.fromString(response.weather.firstOrNull()?.main ?: "Clouds"),
                    temperature = response.main.temp,
                    temperatureUnit = TemperatureUnit.CELSIUS,
                    description = response.weather.firstOrNull()?.description ?: "",
                    humidity = response.main.humidity,
                    windSpeed = response.wind?.speed ?: 0.0,
                    timestamp = Instant.ofEpochSecond(response.dt)
                )
            }
            .onErrorMap { error ->
                when (error) {
                    is WebClientResponseException.NotFound ->
                        RegionNotFoundException(city, "City '$city' not found", error)
                    is WebClientResponseException ->
                        ExternalServiceErrorException("openweather", "Weather service error: ${error.message}", error)
                    else ->
                        ExternalServiceException("openweather", "Weather service is unavailable", error)
                }
            }
    }

    override fun getWeatherByCoordinates(lat: Double, lon: Double): Mono<WeatherCondition> {
        return weatherClient.getWeatherByCoordinates(lat, lon)
            .map { response ->
                WeatherCondition(
                    region = response.name,
                    condition = WeatherType.fromString(response.weather.firstOrNull()?.main ?: "Clouds"),
                    temperature = response.main.temp,
                    temperatureUnit = TemperatureUnit.CELSIUS,
                    description = response.weather.firstOrNull()?.description ?: "",
                    humidity = response.main.humidity,
                    windSpeed = response.wind?.speed ?: 0.0,
                    timestamp = Instant.ofEpochSecond(response.dt)
                )
            }
            .onErrorMap { error ->
                when (error) {
                    is WebClientResponseException.NotFound ->
                        RegionNotFoundException("$lat,$lon", "Coordinates '$lat,$lon' not found", error)
                    is WebClientResponseException ->
                        ExternalServiceErrorException("openweather", "Weather service error: ${error.message}", error)
                    else ->
                        ExternalServiceException("openweather", "Weather service is unavailable", error)
                }
            }
    }

    override fun resolveCoordinates(city: String): Mono<Pair<Double, Double>> {
        return weatherClient.geocode(city, limit = 1)
            .mapNotNull { results ->
                results.firstOrNull()?.let { Pair(it.lat, it.lon) }
            }
            .switchIfEmpty(Mono.error(RegionNotFoundException(city, "Could not resolve coordinates for city '$city'")))
            .onErrorMap { error ->
                when (error) {
                    is RegionNotFoundException -> error
                    is WebClientResponseException ->
                        ExternalServiceErrorException("openweather", "Geocoding service error: ${error.message}", error)
                    else ->
                        ExternalServiceException("openweather", "Geocoding service is unavailable", error)
                }
            }
    }

    override fun isHealthy(): Mono<Boolean> {
        return weatherClient.healthCheck()
    }
}
