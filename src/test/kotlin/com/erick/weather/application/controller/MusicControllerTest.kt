package com.erick.weather.application.controller

import com.erick.weather.application.mapper.MoodMapper
import com.erick.weather.application.mapper.MusicMapper
import com.erick.weather.domain.model.*
import com.erick.weather.domain.service.MoodMusicResult
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

/**
 * Integration tests for MusicController
 */
@WebFluxTest(MusicController::class)
@Import(MusicMapper::class, MoodMapper::class)
class MusicControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var musicRecommendationService: MusicRecommendationService

    @Test
    fun `GET mood-music should return recommendations with default parameters`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Uplifting music",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        val recommendations = listOf(
            MusicRecommendation(
                id = "playlist1",
                name = "Happy Vibes",
                type = RecommendationType.PLAYLIST,
                description = "Feel good music",
                trackCount = 50,
                avgValence = 0.8,
                avgEnergy = 0.7,
                url = "https://spotify.com/playlist1",
                imageUrl = "https://image.url/1.jpg"
            )
        )

        val result = MoodMusicResult(
            region = "Madrid",
            mood = mood,
            recommendations = recommendations,
            total = 1
        )

        whenever(musicRecommendationService.getMoodMusicRecommendations(any(), eq(10), eq(null)))
            .thenReturn(Mono.just(result))

        webTestClient.get()
            .uri("/api/v1/regions/Madrid/mood-music")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.region").isEqualTo("Madrid")
            .jsonPath("$.mood.name").isEqualTo("HAPPY")
            .jsonPath("$.recommendations[0].name").isEqualTo("Happy Vibes")
            .jsonPath("$.recommendations[0].type").isEqualTo("playlist")
            .jsonPath("$.recommendations[0].avgValence").isEqualTo(0.8)
            .jsonPath("$.total").isEqualTo(1)
    }

    @Test
    fun `GET mood-music should accept limit parameter`() {
        val mood = MoodProfile(
            name = MoodType.MELANCHOLIC,
            description = "Introspective music",
            valence = Range(0.0, 0.4),
            energy = Range(0.3, 0.6)
        )

        val result = MoodMusicResult(
            region = "London",
            mood = mood,
            recommendations = emptyList(),
            total = 0
        )

        whenever(musicRecommendationService.getMoodMusicRecommendations(any(), eq(20), eq(null)))
            .thenReturn(Mono.just(result))

        webTestClient.get()
            .uri("/api/v1/regions/London/mood-music?limit=20")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.total").isEqualTo(0)
    }

    @Test
    fun `GET mood-music should accept type parameter for playlists`() {
        val mood = MoodProfile(
            name = MoodType.CALM,
            description = "Relaxing music",
            valence = Range(0.3, 0.6),
            energy = Range(0.2, 0.5)
        )

        val result = MoodMusicResult(
            region = "Paris",
            mood = mood,
            recommendations = emptyList(),
            total = 0
        )

        whenever(musicRecommendationService.getMoodMusicRecommendations(any(), eq(10), eq(RecommendationType.PLAYLIST)))
            .thenReturn(Mono.just(result))

        webTestClient.get()
            .uri("/api/v1/regions/Paris/mood-music?type=playlist")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `GET mood-music should accept type parameter for tracks`() {
        val mood = MoodProfile(
            name = MoodType.INTENSE,
            description = "Dramatic music",
            valence = Range(0.0, 0.3),
            energy = Range(0.6, 1.0)
        )

        val result = MoodMusicResult(
            region = "Berlin",
            mood = mood,
            recommendations = emptyList(),
            total = 0
        )

        whenever(musicRecommendationService.getMoodMusicRecommendations(any(), eq(10), eq(RecommendationType.TRACK)))
            .thenReturn(Mono.just(result))

        webTestClient.get()
            .uri("/api/v1/regions/Berlin/mood-music?type=track")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `GET mood-music should reject invalid type parameter`() {
        webTestClient.get()
            .uri("/api/v1/regions/Madrid/mood-music?type=invalid")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("BAD_REQUEST")
    }

    // Note: Bean Validation (@Min/@Max) requires additional configuration in WebFlux
    // Validation tests are omitted for now
}
