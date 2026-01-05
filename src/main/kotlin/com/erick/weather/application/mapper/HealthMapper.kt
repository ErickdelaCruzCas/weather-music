package com.erick.weather.application.mapper

import com.erick.weather.application.dto.HealthResponse
import com.erick.weather.application.dto.ServiceHealth
import com.erick.weather.application.dto.Services
import com.erick.weather.domain.service.ExternalHealthStatus
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Mapper for converting domain health models to application DTOs
 */
@Component
class HealthMapper {

    fun toHealthResponse(healthStatus: ExternalHealthStatus): HealthResponse {
        return HealthResponse(
            status = healthStatus.status.name.lowercase(),
            timestamp = Instant.now(),
            services = Services(
                spotify = ServiceHealth(
                    status = if (healthStatus.musicServiceHealthy) "up" else "down",
                    responseTime = null, // Could be enhanced with actual response time
                    error = if (!healthStatus.musicServiceHealthy) "Service unavailable" else null
                ),
                openweather = ServiceHealth(
                    status = if (healthStatus.weatherServiceHealthy) "up" else "down",
                    responseTime = null, // Could be enhanced with actual response time
                    error = if (!healthStatus.weatherServiceHealthy) "Service unavailable" else null
                )
            )
        )
    }
}
