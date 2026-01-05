package com.erick.weather.domain.service

import com.erick.weather.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for WeatherMoodMapper
 */
class WeatherMoodMapperTest {

    private val mapper = WeatherMoodMapper()

    @Test
    fun `should map rain to melancholic mood`() {
        val weather = createWeather(WeatherType.RAIN)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.MELANCHOLIC, mood.name)
        assertTrue(mood.valence.min == 0.0 && mood.valence.max == 0.4)
        assertTrue(mood.energy.min == 0.3 && mood.energy.max == 0.6)
    }

    @Test
    fun `should map drizzle to melancholic mood`() {
        val weather = createWeather(WeatherType.DRIZZLE)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.MELANCHOLIC, mood.name)
    }

    @Test
    fun `should map thunderstorm to intense mood`() {
        val weather = createWeather(WeatherType.THUNDERSTORM)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.INTENSE, mood.name)
        assertTrue(mood.valence.min == 0.0 && mood.valence.max == 0.3)
        assertTrue(mood.energy.min == 0.6 && mood.energy.max == 1.0)
    }

    @Test
    fun `should map clear to happy mood`() {
        val weather = createWeather(WeatherType.CLEAR)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.HAPPY, mood.name)
        assertTrue(mood.valence.min == 0.6 && mood.valence.max == 1.0)
        assertTrue(mood.energy.min == 0.5 && mood.energy.max == 0.8)
    }

    @Test
    fun `should map clouds to calm mood`() {
        val weather = createWeather(WeatherType.CLOUDS)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.CALM, mood.name)
    }

    @Test
    fun `should map snow to peaceful mood`() {
        val weather = createWeather(WeatherType.SNOW)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.PEACEFUL, mood.name)
    }

    @Test
    fun `should map mist to mysterious mood`() {
        val weather = createWeather(WeatherType.MIST)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.MYSTERIOUS, mood.name)
    }

    @Test
    fun `should map fog to mysterious mood`() {
        val weather = createWeather(WeatherType.FOG)

        val mood = mapper.mapWeatherToMood(weather)

        assertEquals(MoodType.MYSTERIOUS, mood.name)
    }

    @Test
    fun `should return keywords for melancholic mood`() {
        val mood = MoodProfile(
            name = MoodType.MELANCHOLIC,
            description = "Test",
            valence = Range(0.0, 0.4),
            energy = Range(0.3, 0.6)
        )

        val keywords = mapper.getMoodKeywords(mood)

        assertTrue(keywords.isNotEmpty())
        assertTrue(keywords.contains("rainy day"))
        assertTrue(keywords.contains("chill"))
        assertTrue(keywords.contains("melancholy"))
    }

    @Test
    fun `should return keywords for happy mood`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Test",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        val keywords = mapper.getMoodKeywords(mood)

        assertTrue(keywords.isNotEmpty())
        assertTrue(keywords.contains("happy"))
        assertTrue(keywords.contains("sunny"))
        assertTrue(keywords.contains("upbeat"))
    }

    private fun createWeather(type: WeatherType): WeatherCondition {
        return WeatherCondition(
            region = "TestCity",
            condition = type,
            temperature = 20.0,
            temperatureUnit = TemperatureUnit.CELSIUS,
            description = "test weather",
            humidity = 70,
            windSpeed = 10.0,
            timestamp = Instant.now()
        )
    }
}
