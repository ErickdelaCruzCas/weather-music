package com.erick.weather.application.mapper

import com.erick.weather.application.dto.MoodProfile
import com.erick.weather.application.dto.Range
import org.springframework.stereotype.Component
import com.erick.weather.domain.model.MoodProfile as DomainMoodProfile
import com.erick.weather.domain.model.Range as DomainRange

/**
 * Mapper for converting domain mood models to application DTOs
 */
@Component
class MoodMapper {

    fun toMoodProfileDto(mood: DomainMoodProfile): MoodProfile {
        return MoodProfile(
            name = mood.name.name,
            description = mood.description,
            valence = toRangeDto(mood.valence),
            energy = toRangeDto(mood.energy)
        )
    }

    private fun toRangeDto(range: DomainRange): Range {
        return Range(
            min = range.min,
            max = range.max
        )
    }
}
