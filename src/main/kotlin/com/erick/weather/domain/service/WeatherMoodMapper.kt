package com.erick.weather.domain.service

import com.erick.weather.domain.model.*
import org.springframework.stereotype.Service

/**
 * Maps weather conditions to mood profiles.
 * Implements the weather-to-mood mapping logic defined in the use case.
 */
@Service
class WeatherMoodMapper {

    /**
     * Map weather condition to appropriate mood profile
     */
    fun mapWeatherToMood(weather: WeatherCondition): MoodProfile {
        return when (weather.condition) {
            WeatherType.RAIN, WeatherType.DRIZZLE -> createMelancholicMood()
            WeatherType.THUNDERSTORM -> createIntenseMood()
            WeatherType.CLEAR -> createHappyMood()
            WeatherType.CLOUDS -> createCalmMood()
            WeatherType.SNOW -> createPeacefulMood()
            WeatherType.MIST, WeatherType.FOG -> createMysteriousMood()
        }
    }

    /**
     * Get mood keywords for search queries
     */
    fun getMoodKeywords(mood: MoodProfile): List<String> {
        return when (mood.name) {
            MoodType.MELANCHOLIC -> listOf(
                "rainy day",
                "chill",
                "melancholy",
                "introspective",
                "cozy rain",
                "indie chill",
                "lo-fi"
            )
            MoodType.INTENSE -> listOf(
                "epic",
                "dramatic",
                "intense",
                "dark",
                "powerful",
                "storm"
            )
            MoodType.HAPPY -> listOf(
                "happy",
                "sunny",
                "upbeat",
                "summer vibes",
                "feel good",
                "positive"
            )
            MoodType.CALM -> listOf(
                "calm",
                "peaceful",
                "relaxing",
                "chill",
                "ambient"
            )
            MoodType.PEACEFUL -> listOf(
                "peaceful",
                "winter",
                "serene",
                "quiet",
                "meditation",
                "ambient"
            )
            MoodType.MYSTERIOUS -> listOf(
                "mysterious",
                "ethereal",
                "atmospheric",
                "foggy",
                "ambient"
            )
        }
    }

    // Mood Profile Factories

    private fun createMelancholicMood(): MoodProfile {
        return MoodProfile(
            name = MoodType.MELANCHOLIC,
            description = MoodType.MELANCHOLIC.getDescription(),
            valence = Range(0.0, 0.4),
            energy = Range(0.3, 0.6)
        )
    }

    private fun createIntenseMood(): MoodProfile {
        return MoodProfile(
            name = MoodType.INTENSE,
            description = MoodType.INTENSE.getDescription(),
            valence = Range(0.0, 0.3),
            energy = Range(0.6, 1.0)
        )
    }

    private fun createHappyMood(): MoodProfile {
        return MoodProfile(
            name = MoodType.HAPPY,
            description = MoodType.HAPPY.getDescription(),
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )
    }

    private fun createCalmMood(): MoodProfile {
        return MoodProfile(
            name = MoodType.CALM,
            description = MoodType.CALM.getDescription(),
            valence = Range(0.3, 0.6),
            energy = Range(0.2, 0.5)
        )
    }

    private fun createPeacefulMood(): MoodProfile {
        return MoodProfile(
            name = MoodType.PEACEFUL,
            description = MoodType.PEACEFUL.getDescription(),
            valence = Range(0.4, 0.7),
            energy = Range(0.1, 0.4)
        )
    }

    private fun createMysteriousMood(): MoodProfile {
        return MoodProfile(
            name = MoodType.MYSTERIOUS,
            description = MoodType.MYSTERIOUS.getDescription(),
            valence = Range(0.2, 0.5),
            energy = Range(0.2, 0.5)
        )
    }
}
