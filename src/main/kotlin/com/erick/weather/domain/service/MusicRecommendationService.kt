package com.erick.weather.domain.service

import com.erick.weather.domain.model.*
import com.erick.weather.domain.port.MusicPort
import com.erick.weather.domain.port.WeatherPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Core business logic service for music recommendations based on weather.
 * Orchestrates weather data, mood mapping, and music search.
 */
@Service
class MusicRecommendationService(
    private val weatherPort: WeatherPort,
    private val musicPort: MusicPort,
    private val weatherMoodMapper: WeatherMoodMapper,
    private val regionResolver: RegionResolver
) {

    private val logger = LoggerFactory.getLogger(MusicRecommendationService::class.java)

    /**
     * Get current weather for a region
     */
    fun getWeather(region: String): Mono<WeatherCondition> {
        logger.debug("Getting weather for region: {}", region)

        return when (val identifier = regionResolver.parseRegion(region)) {
            is RegionIdentifier.CityName -> {
                weatherPort.getWeatherByCity(identifier.name)
            }
            is RegionIdentifier.Coordinates -> {
                weatherPort.getWeatherByCoordinates(identifier.lat, identifier.lon)
            }
        }.doOnSuccess { weather ->
            logger.info("Retrieved weather for region '{}': {}, {}°C",
                region, weather.condition, weather.temperature)
        }
    }

    /**
     * Get mood-based music recommendations for a region
     */
    fun getMoodMusicRecommendations(
        region: String,
        limit: Int = 10,
        type: RecommendationType? = null
    ): Mono<MoodMusicResult> {
        logger.debug("Getting mood music for region: {}, limit: {}", region, limit)

        return getWeather(region)
            .flatMap { weather ->
                val mood = weatherMoodMapper.mapWeatherToMood(weather)
                logger.info("Mapped weather '{}' to mood: {}", weather.condition, mood.name)

                getMusicForMood(mood, limit, type)
                    .map { recommendations ->
                        MoodMusicResult(
                            region = region,
                            mood = mood,
                            recommendations = recommendations,
                            total = recommendations.size
                        )
                    }
            }
    }

    /**
     * Get complete weather context with music recommendations
     */
    fun getWeatherContext(
        region: String,
        limit: Int = 10
    ): Mono<WeatherMusicContext> {
        logger.debug("Getting weather context for region: {}", region)

        return getWeather(region)
            .flatMap { weather ->
                val mood = weatherMoodMapper.mapWeatherToMood(weather)

                getMusicForMood(mood, limit)
                    .map { recommendations ->
                        WeatherMusicContext(
                            region = region,
                            weather = weather,
                            mood = mood,
                            recommendations = recommendations,
                            total = recommendations.size
                        )
                    }
            }
    }

    /**
     * Search music matching a mood profile
     */
    private fun getMusicForMood(
        mood: MoodProfile,
        limit: Int,
        type: RecommendationType? = null
    ): Mono<List<MusicRecommendation>> {
        val keywords = weatherMoodMapper.getMoodKeywords(mood)

        logger.debug("Searching music for mood: {} with keywords: {}", mood.name, keywords)

        return musicPort.getRecommendationsForMood(mood, keywords, limit)
            .map { recommendations ->
                // Calculate match scores and sort by best match
                recommendations
                    .map { it.withMatchScore(mood) }
                    .sortedByDescending { it.matchScore }
                    .take(limit)
            }
            .doOnSuccess { recommendations ->
                logger.info("Found {} recommendations for mood: {}", recommendations.size, mood.name)
            }
    }

    /**
     * Check health of external services
     */
    fun checkExternalHealth(): Mono<ExternalHealthStatus> {
        logger.debug("Checking external services health")

        return Mono.zip(
            weatherPort.isHealthy().onErrorReturn(false),
            musicPort.isHealthy().onErrorReturn(false)
        ).map { tuple ->
            val weatherHealthy = tuple.t1
            val musicHealthy = tuple.t2

            val status = when {
                weatherHealthy && musicHealthy -> HealthStatus.HEALTHY
                weatherHealthy || musicHealthy -> HealthStatus.DEGRADED
                else -> HealthStatus.UNHEALTHY
            }

            ExternalHealthStatus(
                status = status,
                weatherServiceHealthy = weatherHealthy,
                musicServiceHealthy = musicHealthy
            )
        }
    }
}

/**
 * Result containing mood and music recommendations
 */
data class MoodMusicResult(
    val region: String,
    val mood: MoodProfile,
    val recommendations: List<MusicRecommendation>,
    val total: Int
)

/**
 * External services health status
 */
data class ExternalHealthStatus(
    val status: HealthStatus,
    val weatherServiceHealthy: Boolean,
    val musicServiceHealthy: Boolean
)

enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY
}
