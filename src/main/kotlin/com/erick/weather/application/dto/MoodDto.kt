package com.erick.weather.application.dto

/**
 * Application DTO for mood profile.
 * Maps to OpenAPI schema: MoodProfile
 */
data class MoodProfile(
    val name: String,
    val description: String,
    val valence: Range,
    val energy: Range
)

/**
 * Application DTO for numeric ranges.
 * Maps to OpenAPI schema: Range
 */
data class Range(
    val min: Double,
    val max: Double
)
