package com.erick.weather.application.mapper

import com.erick.weather.application.dto.WeatherContextResponse
import com.erick.weather.domain.model.WeatherMusicContext
import org.springframework.stereotype.Component

/**
 * Mapper for converting domain context models to application DTOs
 */
@Component
class ContextMapper(
    private val weatherMapper: WeatherMapper,
    private val moodMapper: MoodMapper,
    private val musicMapper: MusicMapper
) {

    fun toWeatherContextResponse(context: WeatherMusicContext): WeatherContextResponse {
        return WeatherContextResponse(
            region = context.region,
            timestamp = context.weather.timestamp,
            weather = weatherMapper.toWeatherDto(context.weather),
            mood = moodMapper.toMoodProfileDto(context.mood),
            recommendations = context.recommendations.map { musicMapper.toMusicRecommendationDto(it) },
            total = context.total
        )
    }
}
