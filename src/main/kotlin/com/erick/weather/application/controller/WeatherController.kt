package com.erick.weather.application.controller

import com.erick.weather.application.dto.WeatherResponse
import com.erick.weather.application.mapper.WeatherMapper
import com.erick.weather.domain.service.MusicRecommendationService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * REST Controller for weather endpoints.
 * Thin orchestration layer - no business logic.
 */
@RestController
@RequestMapping("/api/v1/regions")
class WeatherController(
    private val musicRecommendationService: MusicRecommendationService,
    private val weatherMapper: WeatherMapper
) {

    /**
     * GET /api/v1/regions/{region}/weather
     * Returns current weather for a region
     */
    @GetMapping("/{region}/weather")
    fun getWeather(@PathVariable region: String): Mono<WeatherResponse> {
        return musicRecommendationService.getWeather(region)
            .map { weather -> weatherMapper.toWeatherResponse(region, weather) }
    }
}
