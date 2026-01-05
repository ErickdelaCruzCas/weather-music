package com.erick.weather.domain.service

import org.springframework.stereotype.Service

/**
 * Resolves region identifiers to different formats.
 * Handles city names, coordinates, and validation.
 */
@Service
class RegionResolver {

    /**
     * Parse region string and determine its type
     */
    fun parseRegion(region: String): RegionIdentifier {
        val trimmed = region.trim()

        // Check if it's coordinates (lat,lon)
        val coordinatesPattern = Regex("""^-?\d+\.?\d*,-?\d+\.?\d*$""")
        if (coordinatesPattern.matches(trimmed)) {
            val parts = trimmed.split(",")
            val lat = parts[0].toDoubleOrNull()
            val lon = parts[1].toDoubleOrNull()

            if (lat != null && lon != null && isValidCoordinates(lat, lon)) {
                return RegionIdentifier.Coordinates(lat, lon)
            }
        }

        // Otherwise, treat as city name
        return RegionIdentifier.CityName(trimmed)
    }

    /**
     * Validate coordinates are within valid range
     */
    private fun isValidCoordinates(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }

    /**
     * Normalize city name (handle common formats)
     */
    fun normalizeCityName(city: String): String {
        return city.trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Check if region string looks valid
     */
    fun isValidRegion(region: String): Boolean {
        if (region.isBlank()) return false

        return when (val identifier = parseRegion(region)) {
            is RegionIdentifier.CityName -> identifier.name.length >= 2
            is RegionIdentifier.Coordinates -> true
        }
    }
}

/**
 * Sealed class representing different types of region identifiers
 */
sealed class RegionIdentifier {
    data class CityName(val name: String) : RegionIdentifier()
    data class Coordinates(val lat: Double, val lon: Double) : RegionIdentifier()
}
