package com.erick.weather.domain.exception

/**
 * Base exception for domain errors
 */
sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when a region cannot be found or resolved
 */
class RegionNotFoundException(
    val region: String,
    message: String = "Region '$region' not found",
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Thrown when region format is invalid
 */
class InvalidRegionException(
    val region: String,
    message: String = "Invalid region format: '$region'",
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Thrown when external service is unavailable
 */
class ExternalServiceException(
    val service: String,
    message: String = "External service '$service' is unavailable",
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Thrown when external service returns an error
 */
class ExternalServiceErrorException(
    val service: String,
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)
