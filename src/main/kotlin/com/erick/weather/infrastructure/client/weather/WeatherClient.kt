package com.erick.weather.infrastructure.client.weather

import com.erick.weather.infrastructure.config.OpenWeatherProperties
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class WeatherClient(
    @Qualifier("weatherWebClient") private val webClient: WebClient,
    private val openWeatherProperties: OpenWeatherProperties
) {

    private val logger = LoggerFactory.getLogger(WeatherClient::class.java)

    /**
     * Get current weather by city name
     * @param city City name (e.g., "Madrid", "London,GB")
     * @return Weather data from OpenWeather API
     */
    @CircuitBreaker(name = "weather", fallbackMethod = "getWeatherFallback")
    fun getWeatherByCity(city: String): Mono<OpenWeatherResponse> {
        logger.debug("Fetching weather for city: {}", city)

        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/weather")
                    .queryParam("q", city)
                    .queryParam("appid", openWeatherProperties.apiKey)
                    .queryParam("units", "metric")
                    .build()
            }
            .retrieve()
            .bodyToMono(OpenWeatherResponse::class.java)
            .retryWhen(
                Retry.backoff(2, Duration.ofMillis(500))
                    .filter { it is WebClientResponseException && it.statusCode.is5xxServerError }
            )
            .doOnSuccess { logger.info("Successfully fetched weather for city: {}", city) }
            .doOnError { error -> logger.error("Error fetching weather for city: {}", city, error) }
    }

    /**
     * Get current weather by coordinates
     * @param lat Latitude
     * @param lon Longitude
     * @return Weather data from OpenWeather API
     */
    @CircuitBreaker(name = "weather", fallbackMethod = "getWeatherByCoordinatesFallback")
    fun getWeatherByCoordinates(lat: Double, lon: Double): Mono<OpenWeatherResponse> {
        logger.debug("Fetching weather for coordinates: lat={}, lon={}", lat, lon)

        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/weather")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("appid", openWeatherProperties.apiKey)
                    .queryParam("units", "metric")
                    .build()
            }
            .retrieve()
            .bodyToMono(OpenWeatherResponse::class.java)
            .retryWhen(
                Retry.backoff(2, Duration.ofMillis(500))
                    .filter { it is WebClientResponseException && it.statusCode.is5xxServerError }
            )
            .doOnSuccess { logger.info("Successfully fetched weather for coordinates: lat={}, lon={}", lat, lon) }
            .doOnError { error -> logger.error("Error fetching weather for coordinates: lat={}, lon={}", lat, lon, error) }
    }

    /**
     * Geocoding: Convert city name to coordinates
     * @param city City name
     * @param limit Maximum number of results
     * @return List of geocoding results
     */
    fun geocode(city: String, limit: Int = 5): Mono<List<GeocodingResponse>> {
        logger.debug("Geocoding city: {}", city)

        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .scheme("http")
                    .host("api.openweathermap.org")
                    .path("/geo/1.0/direct")
                    .queryParam("q", city)
                    .queryParam("limit", limit)
                    .queryParam("appid", openWeatherProperties.apiKey)
                    .build()
            }
            .retrieve()
            .bodyToFlux(GeocodingResponse::class.java)
            .collectList()
            .doOnSuccess { logger.info("Successfully geocoded city: {} ({} results)", city, it.size) }
            .doOnError { error -> logger.error("Error geocoding city: {}", city, error) }
    }

    /**
     * Health check for OpenWeather API
     * @return True if service is available
     */
    fun healthCheck(): Mono<Boolean> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/weather")
                    .queryParam("q", "London")
                    .queryParam("appid", openWeatherProperties.apiKey)
                    .build()
            }
            .retrieve()
            .toBodilessEntity()
            .map { true }
            .onErrorReturn(false)
            .timeout(Duration.ofSeconds(3))
    }

    // Fallback methods for Circuit Breaker

    private fun getWeatherFallback(city: String, throwable: Throwable): Mono<OpenWeatherResponse> {
        logger.warn("Weather service fallback triggered for city: {}. Reason: {}", city, throwable.message)
        return Mono.error(WeatherServiceException("Weather service temporarily unavailable", throwable))
    }

    private fun getWeatherByCoordinatesFallback(lat: Double, lon: Double, throwable: Throwable): Mono<OpenWeatherResponse> {
        logger.warn("Weather service fallback triggered for coordinates: lat={}, lon={}. Reason: {}", lat, lon, throwable.message)
        return Mono.error(WeatherServiceException("Weather service temporarily unavailable", throwable))
    }
}

class WeatherServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
