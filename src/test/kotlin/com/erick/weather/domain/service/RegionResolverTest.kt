package com.erick.weather.domain.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for RegionResolver
 */
class RegionResolverTest {

    private val resolver = RegionResolver()

    @Test
    fun `should parse city name`() {
        val result = resolver.parseRegion("Madrid")

        assertTrue(result is RegionIdentifier.CityName)
        assertEquals("Madrid", (result as RegionIdentifier.CityName).name)
    }

    @Test
    fun `should parse city name with spaces`() {
        val result = resolver.parseRegion("New York")

        assertTrue(result is RegionIdentifier.CityName)
        assertEquals("New York", (result as RegionIdentifier.CityName).name)
    }

    @Test
    fun `should parse valid coordinates`() {
        val result = resolver.parseRegion("40.4168,-3.7038")

        assertTrue(result is RegionIdentifier.Coordinates)
        val coords = result as RegionIdentifier.Coordinates
        assertEquals(40.4168, coords.lat, 0.0001)
        assertEquals(-3.7038, coords.lon, 0.0001)
    }

    @Test
    fun `should parse negative coordinates`() {
        val result = resolver.parseRegion("-33.8688,151.2093")

        assertTrue(result is RegionIdentifier.Coordinates)
        val coords = result as RegionIdentifier.Coordinates
        assertEquals(-33.8688, coords.lat, 0.0001)
        assertEquals(151.2093, coords.lon, 0.0001)
    }

    @Test
    fun `should parse coordinates without decimals`() {
        val result = resolver.parseRegion("40,-3")

        assertTrue(result is RegionIdentifier.Coordinates)
        val coords = result as RegionIdentifier.Coordinates
        assertEquals(40.0, coords.lat, 0.0001)
        assertEquals(-3.0, coords.lon, 0.0001)
    }

    @Test
    fun `should reject invalid coordinates - out of range latitude`() {
        val result = resolver.parseRegion("100.0,50.0")

        assertTrue(result is RegionIdentifier.CityName)
    }

    @Test
    fun `should reject invalid coordinates - out of range longitude`() {
        val result = resolver.parseRegion("50.0,200.0")

        assertTrue(result is RegionIdentifier.CityName)
    }

    @Test
    fun `should normalize city name capitalization`() {
        val normalized = resolver.normalizeCityName("madrid")

        assertEquals("Madrid", normalized)
    }

    @Test
    fun `should preserve already capitalized city name`() {
        val normalized = resolver.normalizeCityName("Madrid")

        assertEquals("Madrid", normalized)
    }

    @Test
    fun `should trim whitespace from city name`() {
        val normalized = resolver.normalizeCityName("  Madrid  ")

        assertEquals("Madrid", normalized)
    }

    @Test
    fun `should validate valid city name`() {
        assertTrue(resolver.isValidRegion("Madrid"))
        assertTrue(resolver.isValidRegion("New York"))
    }

    @Test
    fun `should validate valid coordinates`() {
        assertTrue(resolver.isValidRegion("40.4168,-3.7038"))
        assertTrue(resolver.isValidRegion("-33.8688,151.2093"))
    }

    @Test
    fun `should reject blank region`() {
        assertFalse(resolver.isValidRegion(""))
        assertFalse(resolver.isValidRegion("   "))
    }

    @Test
    fun `should reject very short city name`() {
        assertFalse(resolver.isValidRegion("A"))
    }

    @Test
    fun `should accept two-letter city name`() {
        assertTrue(resolver.isValidRegion("AB"))
    }
}
