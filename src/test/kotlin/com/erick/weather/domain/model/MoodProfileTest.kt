package com.erick.weather.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for MoodProfile matching and scoring algorithms
 */
class MoodProfileTest {

    @Test
    fun `should match when both valence and energy are within range`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        assertTrue(mood.matches(valence = 0.7, energy = 0.6))
        assertTrue(mood.matches(valence = 0.6, energy = 0.5)) // Boundaries
        assertTrue(mood.matches(valence = 1.0, energy = 0.8)) // Boundaries
    }

    @Test
    fun `should not match when valence is out of range`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        assertFalse(mood.matches(valence = 0.3, energy = 0.6))
        assertFalse(mood.matches(valence = 0.5, energy = 0.6))
    }

    @Test
    fun `should not match when energy is out of range`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        assertFalse(mood.matches(valence = 0.7, energy = 0.3))
        assertFalse(mood.matches(valence = 0.7, energy = 0.9))
    }

    @Test
    fun `should calculate perfect match score`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        // Center of the range
        val score = mood.matchScore(valence = 0.8, energy = 0.65)

        assertTrue(score >= 0.9) // Should be near 1.0 for center values
    }

    @Test
    fun `should calculate lower match score for boundary values`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        // Boundary values
        val score = mood.matchScore(valence = 0.6, energy = 0.5)

        assertTrue(score > 0.0) // Should have some score at boundaries
        assertTrue(score <= 1.0)
    }

    @Test
    fun `should calculate zero score for values far outside range`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        // Very far from range
        val score = mood.matchScore(valence = 0.1, energy = 0.1)

        assertTrue(score >= 0.0) // Should be >= 0
        assertTrue(score < 1.0) // Should be less than perfect match
    }

    @Test
    fun `Range should check if value is within boundaries`() {
        val range = Range(0.3, 0.7)

        assertTrue(range.contains(0.5))
        assertTrue(range.contains(0.3)) // Boundary
        assertTrue(range.contains(0.7)) // Boundary
        assertFalse(range.contains(0.2))
        assertFalse(range.contains(0.8))
    }

    @Test
    fun `Range should calculate proximity score for value inside range`() {
        val range = Range(0.3, 0.7)

        // Value inside range should have score 1.0
        val score = range.proximityScore(0.5)

        assertEquals(1.0, score, 0.001)
    }

    @Test
    fun `Range should calculate proximity score for value outside range`() {
        val range = Range(0.3, 0.7)

        // Value outside range should have score < 1.0
        val score = range.proximityScore(0.2)

        assertTrue(score < 1.0)
        assertTrue(score > 0.0) // But not zero if not too far
    }

    @Test
    fun `MusicRecommendation should calculate match score based on mood`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        val recommendation = MusicRecommendation(
            id = "test123",
            name = "Happy Song",
            type = RecommendationType.TRACK,
            description = "A happy track",
            trackCount = null,
            avgValence = 0.8,
            avgEnergy = 0.7,
            url = "https://spotify.com/track/test123",
            imageUrl = null
        )

        val withScore = recommendation.withMatchScore(mood)

        assertNotNull(withScore.matchScore)
        assertTrue(withScore.matchScore!! > 0.8) // Should match well
    }

    @Test
    fun `MusicRecommendation should filter by mood profile`() {
        val mood = MoodProfile(
            name = MoodType.HAPPY,
            description = "Happy mood",
            valence = Range(0.6, 1.0),
            energy = Range(0.5, 0.8)
        )

        val matchingRecommendation = MusicRecommendation(
            id = "test123",
            name = "Happy Song",
            type = RecommendationType.TRACK,
            description = "A happy track",
            trackCount = null,
            avgValence = 0.8,
            avgEnergy = 0.7,
            url = "https://spotify.com/track/test123",
            imageUrl = null
        )

        val nonMatchingRecommendation = MusicRecommendation(
            id = "test456",
            name = "Sad Song",
            type = RecommendationType.TRACK,
            description = "A sad track",
            trackCount = null,
            avgValence = 0.2,
            avgEnergy = 0.3,
            url = "https://spotify.com/track/test456",
            imageUrl = null
        )

        assertTrue(matchingRecommendation.fitsProfile(mood))
        assertFalse(nonMatchingRecommendation.fitsProfile(mood))
    }
}
