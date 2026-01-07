package com.erick.weather.application.controller

import com.erick.weather.application.mapper.HealthMapper
import com.erick.weather.domain.service.ExternalHealthStatus
import com.erick.weather.domain.service.HealthStatus
import com.erick.weather.domain.service.MusicRecommendationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

/**
 * Integration tests for HealthController
 */
@WebFluxTest(HealthController::class)
@Import(HealthMapper::class)
class HealthControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var musicRecommendationService: MusicRecommendationService

    @Test
    fun `GET external health should return HEALTHY when all services are up`() {
        val healthStatus = ExternalHealthStatus(
            status = HealthStatus.HEALTHY,
            weatherServiceHealthy = true,
            musicServiceHealthy = true
        )

        whenever(musicRecommendationService.checkExternalHealth())
            .thenReturn(Mono.just(healthStatus))

        webTestClient.get()
            .uri("/api/v1/health/external")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("healthy")
            .jsonPath("$.services.openweather.status").isEqualTo("up")
            .jsonPath("$.services.spotify.status").isEqualTo("up")
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `GET external health should return DEGRADED when one service is down`() {
        val healthStatus = ExternalHealthStatus(
            status = HealthStatus.DEGRADED,
            weatherServiceHealthy = true,
            musicServiceHealthy = false
        )

        whenever(musicRecommendationService.checkExternalHealth())
            .thenReturn(Mono.just(healthStatus))

        webTestClient.get()
            .uri("/api/v1/health/external")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("degraded")
            .jsonPath("$.services.openweather.status").isEqualTo("up")
            .jsonPath("$.services.spotify.status").isEqualTo("down")
            .jsonPath("$.services.spotify.error").isEqualTo("Service unavailable")
    }

    @Test
    fun `GET external health should return UNHEALTHY when all services are down`() {
        val healthStatus = ExternalHealthStatus(
            status = HealthStatus.UNHEALTHY,
            weatherServiceHealthy = false,
            musicServiceHealthy = false
        )

        whenever(musicRecommendationService.checkExternalHealth())
            .thenReturn(Mono.just(healthStatus))

        webTestClient.get()
            .uri("/api/v1/health/external")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("unhealthy")
            .jsonPath("$.services.openweather.status").isEqualTo("down")
            .jsonPath("$.services.spotify.status").isEqualTo("down")
            .jsonPath("$.services.openweather.error").isEqualTo("Service unavailable")
            .jsonPath("$.services.spotify.error").isEqualTo("Service unavailable")
    }

    @Test
    fun `GET external health should return DEGRADED when weather service is down`() {
        val healthStatus = ExternalHealthStatus(
            status = HealthStatus.DEGRADED,
            weatherServiceHealthy = false,
            musicServiceHealthy = true
        )

        whenever(musicRecommendationService.checkExternalHealth())
            .thenReturn(Mono.just(healthStatus))

        webTestClient.get()
            .uri("/api/v1/health/external")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("degraded")
            .jsonPath("$.services.openweather.status").isEqualTo("down")
            .jsonPath("$.services.spotify.status").isEqualTo("up")
            .jsonPath("$.services.openweather.error").isEqualTo("Service unavailable")
    }
}