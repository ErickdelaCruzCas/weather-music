package com.erick.weather.application.controller

import com.erick.weather.application.dto.WeatherContextResponse
import com.erick.weather.application.mapper.ContextMapper
import com.erick.weather.domain.service.MusicRecommendationService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * REST Controller for aggregated context endpoints.
 * Thin orchestration layer - no business logic.
 */
@RestController
@RequestMapping("/api/v1/regions")
class ContextController(
    private val musicRecommendationService: MusicRecommendationService,
    private val contextMapper: ContextMapper
) {

    /**
     * GET /api/v1/regions/{region}/weather-context
     * Returns aggregated weather context with music recommendations
     */
    @GetMapping("/{region}/weather-context")
    fun getWeatherContext(
        @PathVariable region: String,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) limit: Int
    ): Mono<WeatherContextResponse> {
        return musicRecommendationService.getWeatherContext(region, limit)
            .map { context -> contextMapper.toWeatherContextResponse(context) }
    }
}
