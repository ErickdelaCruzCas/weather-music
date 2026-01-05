package com.erick.weather.application.controller

import com.erick.weather.application.mapper.WeatherMapper
import com.erick.weather.domain.exception.RegionNotFoundException
import com.erick.weather.domain.model.TemperatureUnit
import com.erick.weather.domain.model.WeatherCondition
import com.erick.weather.domain.model.WeatherType
import com.erick.weather.domain.service.MusicRecommendationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Integration tests for WeatherController
 */
@WebFluxTest(WeatherController::class)
@Import(WeatherMapper::class)
class WeatherControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var musicRecommendationService: MusicRecommendationService

    @Test
    fun `GET weather should return weather data for valid city`() {
        val weatherCondition = WeatherCondition(
            region = "Madrid",
            condition = WeatherType.CLEAR,
            temperature = 25.0,
            temperatureUnit = TemperatureUnit.CELSIUS,
            description = "clear sky",
            humidity = 45,
            windSpeed = 10.0,
            timestamp = Instant.parse("2026-01-05T12:00:00Z")
        )

        whenever(musicRecommendationService.getWeather(any()))
            .thenReturn(Mono.just(weatherCondition))

        webTestClient.get()
            .uri("/api/v1/regions/Madrid/weather")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.region").isEqualTo("Madrid")
            .jsonPath("$.weather.condition").isEqualTo("CLEAR")
            .jsonPath("$.weather.temperature").isEqualTo(25.0)
            .jsonPath("$.weather.temperatureUnit").isEqualTo("celsius")
            .jsonPath("$.weather.description").isEqualTo("clear sky")
            .jsonPath("$.weather.humidity").isEqualTo(45)
            .jsonPath("$.weather.windSpeed").isEqualTo(10.0)
    }

    @Test
    fun `GET weather should return 404 for unknown city`() {
        whenever(musicRecommendationService.getWeather(any()))
            .thenReturn(Mono.error(RegionNotFoundException("UnknownCity")))

        webTestClient.get()
            .uri("/api/v1/regions/UnknownCity/weather")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isEqualTo("NOT_FOUND")
            .jsonPath("$.message").exists()
    }

    @Test
    fun `GET weather should handle coordinates`() {
        val weatherCondition = WeatherCondition(
            region = "Barcelona",
            condition = WeatherType.RAIN,
            temperature = 18.0,
            temperatureUnit = TemperatureUnit.CELSIUS,
            description = "light rain",
            humidity = 75,
            windSpeed = 15.0,
            timestamp = Instant.parse("2026-01-05T12:00:00Z")
        )

        whenever(musicRecommendationService.getWeather(any()))
            .thenReturn(Mono.just(weatherCondition))

        webTestClient.get()
            .uri("/api/v1/regions/41.3851,2.1734/weather")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.weather.condition").isEqualTo("RAIN")
    }
}
