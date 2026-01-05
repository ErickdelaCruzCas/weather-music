package com.erick.weather.application.dto

import java.time.Instant

/**
 * Application DTO for error responses.
 * Maps to OpenAPI schema: Error
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, Any>? = null
)
