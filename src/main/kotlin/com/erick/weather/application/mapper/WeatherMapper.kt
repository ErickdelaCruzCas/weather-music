package com.erick.weather.application.mapper

import com.erick.weather.application.dto.Weather
import com.erick.weather.application.dto.WeatherResponse
import com.erick.weather.domain.model.WeatherCondition
import org.springframework.stereotype.Component

/**
 * Mapper for converting domain weather models to application DTOs
 */
@Component
class WeatherMapper {

    fun toWeatherResponse(region: String, weather: WeatherCondition): WeatherResponse {
        return WeatherResponse(
            region = region,
            weather = toWeatherDto(weather)
        )
    }

    fun toWeatherDto(weather: WeatherCondition): Weather {
        return Weather(
            condition = weather.condition.name,
            temperature = weather.temperature,
            temperatureUnit = weather.temperatureUnit.name.lowercase(),
            description = weather.description,
            humidity = weather.humidity,
            windSpeed = weather.windSpeed,
            timestamp = weather.timestamp
        )
    }
}
