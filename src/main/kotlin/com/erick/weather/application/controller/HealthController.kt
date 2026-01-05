package com.erick.weather.application.controller

import com.erick.weather.application.dto.HealthResponse
import com.erick.weather.application.mapper.HealthMapper
import com.erick.weather.domain.service.MusicRecommendationService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * REST Controller for health check endpoints.
 * Thin orchestration layer - no business logic.
 */
@RestController
@RequestMapping("/api/v1/health")
class HealthController(
    private val musicRecommendationService: MusicRecommendationService,
    private val healthMapper: HealthMapper
) {

    /**
     * GET /api/v1/health/external
     * Returns health status of external API dependencies
     */
    @GetMapping("/external")
    fun getExternalHealth(): Mono<HealthResponse> {
        return musicRecommendationService.checkExternalHealth()
            .map { healthStatus -> healthMapper.toHealthResponse(healthStatus) }
    }
}
