package com.erick.weather.application.exception

import com.erick.weather.application.dto.ErrorResponse
import com.erick.weather.domain.exception.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Global exception handler for REST controllers.
 * Converts domain exceptions to appropriate HTTP responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handle region not found errors (404)
     */
    @ExceptionHandler(RegionNotFoundException::class)
    fun handleRegionNotFound(ex: RegionNotFoundException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn("Region not found: {}", ex.region)

        val error = ErrorResponse(
            error = "NOT_FOUND",
            message = ex.message ?: "Region not found",
            timestamp = Instant.now()
        )

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error))
    }

    /**
     * Handle invalid region format errors (400)
     */
    @ExceptionHandler(InvalidRegionException::class)
    fun handleInvalidRegion(ex: InvalidRegionException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn("Invalid region format: {}", ex.region)

        val error = ErrorResponse(
            error = "BAD_REQUEST",
            message = ex.message ?: "Invalid region format",
            timestamp = Instant.now()
        )

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error))
    }

    /**
     * Handle external service unavailable errors (503)
     */
    @ExceptionHandler(ExternalServiceException::class)
    fun handleExternalServiceUnavailable(ex: ExternalServiceException): Mono<ResponseEntity<ErrorResponse>> {
        logger.error("External service unavailable: {}", ex.service, ex)

        val error = ErrorResponse(
            error = "SERVICE_UNAVAILABLE",
            message = ex.message ?: "External service is unavailable",
            timestamp = Instant.now(),
            details = mapOf("service" to ex.service)
        )

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error))
    }

    /**
     * Handle external service errors (503)
     */
    @ExceptionHandler(ExternalServiceErrorException::class)
    fun handleExternalServiceError(ex: ExternalServiceErrorException): Mono<ResponseEntity<ErrorResponse>> {
        logger.error("External service error: {}", ex.service, ex)

        val error = ErrorResponse(
            error = "SERVICE_UNAVAILABLE",
            message = ex.message ?: "External service returned an error",
            timestamp = Instant.now(),
            details = mapOf("service" to ex.service)
        )

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error))
    }

    /**
     * Handle validation errors (400)
     */
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationError(ex: WebExchangeBindException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn("Validation error: {}", ex.message)

        val fieldErrors = ex.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }

        val error = ErrorResponse(
            error = "BAD_REQUEST",
            message = "Validation failed",
            timestamp = Instant.now(),
            details = mapOf("fieldErrors" to fieldErrors)
        )

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error))
    }

    /**
     * Handle invalid request parameters (400)
     */
    @ExceptionHandler(ServerWebInputException::class, IllegalArgumentException::class)
    fun handleBadRequest(ex: Exception): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn("Bad request: {}", ex.message)

        val error = ErrorResponse(
            error = "BAD_REQUEST",
            message = ex.message ?: "Invalid request parameters",
            timestamp = Instant.now()
        )

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error))
    }

    /**
     * Handle all other unexpected errors (500)
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericError(ex: Exception): Mono<ResponseEntity<ErrorResponse>> {
        logger.error("Unexpected error occurred", ex)

        val error = ErrorResponse(
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred. Please try again later.",
            timestamp = Instant.now()
        )

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error))
    }
}
