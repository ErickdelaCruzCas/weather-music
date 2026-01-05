package com.erick.weather.application.controller

import com.erick.weather.application.dto.MoodMusicResponse
import com.erick.weather.application.mapper.MusicMapper
import com.erick.weather.domain.model.RecommendationType
import com.erick.weather.domain.service.MusicRecommendationService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * REST Controller for music recommendation endpoints.
 * Thin orchestration layer - no business logic.
 */
@RestController
@RequestMapping("/api/v1/regions")
class MusicController(
    private val musicRecommendationService: MusicRecommendationService,
    private val musicMapper: MusicMapper
) {

    /**
     * GET /api/v1/regions/{region}/mood-music
     * Returns music recommendations based on weather mood
     */
    @GetMapping("/{region}/mood-music")
    fun getMoodMusic(
        @PathVariable region: String,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) limit: Int,
        @RequestParam(defaultValue = "both") type: String
    ): Mono<MoodMusicResponse> {
        val recommendationType = parseRecommendationType(type)

        return musicRecommendationService.getMoodMusicRecommendations(region, limit, recommendationType)
            .map { result -> musicMapper.toMoodMusicResponse(result) }
    }

    /**
     * Parse query param type to domain RecommendationType
     */
    private fun parseRecommendationType(type: String): RecommendationType? {
        return when (type.lowercase()) {
            "playlist" -> RecommendationType.PLAYLIST
            "track" -> RecommendationType.TRACK
            "both" -> null // null means both types
            else -> throw IllegalArgumentException("Invalid type: $type. Must be one of: playlist, track, both")
        }
    }
}
