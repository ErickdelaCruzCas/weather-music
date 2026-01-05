package com.erick.weather.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.spotify")
data class SpotifyProperties(
    val baseUrl: String,
    val authUrl: String,
    val clientId: String,
    val clientSecret: String
)

@ConfigurationProperties(prefix = "external.openweather")
data class OpenWeatherProperties(
    val baseUrl: String,
    val apiKey: String
)
