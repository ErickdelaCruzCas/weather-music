package com.erick.weather.domain.model

/**
 * Represents a mood profile with valence and energy characteristics.
 * Used to match music to emotional states derived from weather conditions.
 */
data class MoodProfile(
    val name: MoodType,
    val description: String,
    val valence: Range,
    val energy: Range
) {
    /**
     * Check if audio features match this mood profile
     */
    fun matches(valence: Double, energy: Double): Boolean {
        return this.valence.contains(valence) && this.energy.contains(energy)
    }

    /**
     * Calculate match score (0.0 - 1.0) based on how well features fit the mood
     */
    fun matchScore(valence: Double, energy: Double): Double {
        val valenceScore = this.valence.proximityScore(valence)
        val energyScore = this.energy.proximityScore(energy)
        return (valenceScore + energyScore) / 2.0
    }
}

enum class MoodType(val displayName: String) {
    MELANCHOLIC("Melancholic"),
    INTENSE("Intense"),
    HAPPY("Happy"),
    CALM("Calm"),
    PEACEFUL("Peaceful"),
    MYSTERIOUS("Mysterious");

    fun getDescription(): String = when (this) {
        MELANCHOLIC -> "Introspective and contemplative music"
        INTENSE -> "Dramatic and powerful music"
        HAPPY -> "Uplifting and energetic music"
        CALM -> "Relaxed and mellow music"
        PEACEFUL -> "Serene and quiet music"
        MYSTERIOUS -> "Ethereal and atmospheric music"
    }
}

/**
 * Represents a numeric range (e.g., valence or energy range)
 */
data class Range(
    val min: Double,
    val max: Double
) {
    init {
        require(min in 0.0..1.0) { "Min must be between 0.0 and 1.0" }
        require(max in 0.0..1.0) { "Max must be between 0.0 and 1.0" }
        require(min <= max) { "Min must be less than or equal to max" }
    }

    /**
     * Check if value is within range
     */
    fun contains(value: Double): Boolean {
        return value in min..max
    }

    /**
     * Calculate proximity score (0.0 - 1.0)
     * 1.0 = perfect match (within range)
     * < 1.0 = outside range, score decreases with distance
     */
    fun proximityScore(value: Double): Double {
        return when {
            value in min..max -> 1.0
            value < min -> 1.0 - ((min - value).coerceAtMost(1.0))
            else -> 1.0 - ((value - max).coerceAtMost(1.0))
        }
    }

    /**
     * Calculate center point of range
     */
    fun center(): Double = (min + max) / 2.0
}
