package com.erick.weather.application.dto

import java.time.Instant

/**
 * Application DTO for weather context response.
 * Aggregates weather, mood, and music recommendations.
 * Maps to OpenAPI schema: WeatherContextResponse
 */
data class WeatherContextResponse(
    val region: String,
    val timestamp: Instant,
    val weather: Weather,
    val mood: MoodProfile,
    val recommendations: List<MusicRecommendation>,
    val total: Int
)
