package com.erick.weather.application.dto

/**
 * Application DTO for mood-based music response.
 * Maps to OpenAPI schema: MoodMusicResponse
 */
data class MoodMusicResponse(
    val region: String,
    val mood: MoodProfile,
    val recommendations: List<MusicRecommendation>,
    val total: Int
)

/**
 * Application DTO for music recommendation.
 * Maps to OpenAPI schema: MusicRecommendation
 */
data class MusicRecommendation(
    val id: String,
    val name: String,
    val type: String,
    val description: String? = null,
    val trackCount: Int? = null,
    val avgValence: Double,
    val avgEnergy: Double,
    val url: String,
    val imageUrl: String? = null
)
