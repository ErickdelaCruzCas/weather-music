package com.erick.weather.application.mapper

import com.erick.weather.application.dto.MoodMusicResponse
import com.erick.weather.application.dto.MusicRecommendation
import com.erick.weather.domain.service.MoodMusicResult
import org.springframework.stereotype.Component
import com.erick.weather.domain.model.MusicRecommendation as DomainMusicRecommendation

/**
 * Mapper for converting domain music models to application DTOs
 */
@Component
class MusicMapper(
    private val moodMapper: MoodMapper
) {

    fun toMoodMusicResponse(result: MoodMusicResult): MoodMusicResponse {
        return MoodMusicResponse(
            region = result.region,
            mood = moodMapper.toMoodProfileDto(result.mood),
            recommendations = result.recommendations.map { toMusicRecommendationDto(it) },
            total = result.total
        )
    }

    fun toMusicRecommendationDto(recommendation: DomainMusicRecommendation): MusicRecommendation {
        return MusicRecommendation(
            id = recommendation.id,
            name = recommendation.name,
            type = recommendation.type.name.lowercase(),
            description = recommendation.description,
            trackCount = recommendation.trackCount,
            avgValence = recommendation.avgValence,
            avgEnergy = recommendation.avgEnergy,
            url = recommendation.url,
            imageUrl = recommendation.imageUrl
        )
    }
}
