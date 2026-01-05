package com.erick.weather.domain.port

import com.erick.weather.domain.model.MoodProfile
import com.erick.weather.domain.model.MusicRecommendation
import com.erick.weather.domain.model.RecommendationType
import reactor.core.publisher.Mono

/**
 * Port (interface) for music data access.
 * Implementation will be in infrastructure layer (SpotifyClient adapter).
 */
interface MusicPort {

    /**
     * Search music by query and type
     */
    fun searchMusic(
        query: String,
        type: RecommendationType,
        limit: Int
    ): Mono<List<MusicRecommendation>>

    /**
     * Search music that matches a mood profile
     */
    fun searchMusicByMood(
        mood: MoodProfile,
        limit: Int,
        type: RecommendationType? = null
    ): Mono<List<MusicRecommendation>>

    /**
     * Get recommendations with audio features matching mood
     */
    fun getRecommendationsForMood(
        mood: MoodProfile,
        keywords: List<String>,
        limit: Int
    ): Mono<List<MusicRecommendation>>

    /**
     * Health check
     */
    fun isHealthy(): Mono<Boolean>
}
