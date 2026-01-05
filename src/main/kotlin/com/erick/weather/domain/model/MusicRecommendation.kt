package com.erick.weather.domain.model

/**
 * Domain model representing a music recommendation (playlist or track).
 * Independent of external API models (Spotify).
 */
data class MusicRecommendation(
    val id: String,
    val name: String,
    val type: RecommendationType,
    val description: String?,
    val trackCount: Int?,
    val avgValence: Double,
    val avgEnergy: Double,
    val url: String,
    val imageUrl: String?,
    val matchScore: Double? = null
) {
    /**
     * Check if this recommendation fits a mood profile
     */
    fun fitsProfile(profile: MoodProfile): Boolean {
        return profile.matches(avgValence, avgEnergy)
    }

    /**
     * Calculate how well this recommendation matches a mood profile
     */
    fun calculateMatchScore(profile: MoodProfile): Double {
        return profile.matchScore(avgValence, avgEnergy)
    }

    /**
     * Create a copy with match score calculated
     */
    fun withMatchScore(profile: MoodProfile): MusicRecommendation {
        return copy(matchScore = calculateMatchScore(profile))
    }
}

enum class RecommendationType {
    PLAYLIST,
    TRACK;

    companion object {
        fun fromString(value: String): RecommendationType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: PLAYLIST
        }
    }
}

/**
 * Aggregated result containing weather context and music recommendations
 */
data class WeatherMusicContext(
    val region: String,
    val weather: WeatherCondition,
    val mood: MoodProfile,
    val recommendations: List<MusicRecommendation>,
    val total: Int
)
