package com.erick.weather.application.controller

import com.erick.weather.application.mapper.ContextMapper
import com.erick.weather.application.mapper.MoodMapper
import com.erick.weather.application.mapper.MusicMapper
import com.erick.weather.application.mapper.WeatherMapper
import com.erick.weather.domain.model.*
import com.erick.weather.domain.service.MusicRecommendationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Integration tests for ContextController
 */
@WebFluxTest(ContextController::class)
@Import(ContextMapper::class, WeatherMapper::class, MoodMapper::class, MusicMapper::class)
class ContextControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var musicRecommendationService: MusicRecommendationService

    @Test
    fun `GET weather-context should return complete context`() {
        val weather = WeatherCondition(
            region = "Madrid",
            condition = WeatherType.RAIN,
            temperature = 15.0,
            temperatureUnit = TemperatureUnit.CELSIUS,
            description = "light rain",
            humidity = 85,
            windSpeed = 12.0,
            timestamp = Instant.parse("2026-01-05T12:00:00Z")
        )

        val mood = MoodProfile(
            name = MoodType.MELANCHOLIC,
            description = "Introspective music",
            valence = Range(0.0, 0.4),
            energy = Range(0.3, 0.6)
        )

        val recommendations = listOf(
            MusicRecommendation(
                id = "playlist1",
                name = "Rainy Day",
                type = RecommendationType.PLAYLIST,
                description = "Cozy rainy vibes",
                trackCount = 50,
                avgValence = 0.32,
                avgEnergy = 0.45,
                url = "https://spotify.com/playlist1",
                imageUrl = "https://image.url/1.jpg"
            )
        )

        val context = WeatherMusicContext(
            region = "Madrid",
            weather = weather,
            mood = mood,
            recommendations = recommendations,
            total = 1
        )

        whenever(musicRecommendationService.getWeatherContext(any(), eq(10)))
            .thenReturn(Mono.just(context))

        webTestClient.get()
            .uri("/api/v1/regions/Madrid/weather-context")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.region").isEqualTo("Madrid")
            .jsonPath("$.weather.condition").isEqualTo("RAIN")
            .jsonPath("$.weather.temperature").isEqualTo(15.0)
            .jsonPath("$.mood.name").isEqualTo("MELANCHOLIC")
            .jsonPath("$.recommendations[0].name").isEqualTo("Rainy Day")
            .jsonPath("$.total").isEqualTo(1)
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `GET weather-context should accept custom limit`() {
        val context = WeatherMusicContext(
            region = "London",
            weather = WeatherCondition(
                region = "London",
                condition = WeatherType.CLOUDS,
                temperature = 12.0,
                temperatureUnit = TemperatureUnit.CELSIUS,
                description = "overcast clouds",
                humidity = 70,
                windSpeed = 8.0,
                timestamp = Instant.now()
            ),
            mood = MoodProfile(
                name = MoodType.CALM,
                description = "Relaxing music",
                valence = Range(0.3, 0.6),
                energy = Range(0.2, 0.5)
            ),
            recommendations = emptyList(),
            total = 0
        )

        whenever(musicRecommendationService.getWeatherContext(any(), eq(25)))
            .thenReturn(Mono.just(context))

        webTestClient.get()
            .uri("/api/v1/regions/London/weather-context?limit=25")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.total").isEqualTo(0)
    }

    // Note: Bean Validation (@Min/@Max) requires additional configuration in WebFlux
    // Validation tests are omitted for now
}
