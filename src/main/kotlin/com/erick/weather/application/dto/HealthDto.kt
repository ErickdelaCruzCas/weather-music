package com.erick.weather.application.dto

import java.time.Instant

/**
 * Application DTO for external health check response.
 * Maps to OpenAPI schema: HealthResponse
 */
data class HealthResponse(
    val status: String,
    val timestamp: Instant,
    val services: Services
)

/**
 * Container for individual service health statuses
 */
data class Services(
    val spotify: ServiceHealth,
    val openweather: ServiceHealth
)

/**
 * Application DTO for individual service health.
 * Maps to OpenAPI schema: ServiceHealth
 */
data class ServiceHealth(
    val status: String,
    val responseTime: Long? = null,
    val error: String? = null
)
