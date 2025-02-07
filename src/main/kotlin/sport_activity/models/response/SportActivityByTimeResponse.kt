package ru.alexbur.backend.sport_activity.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SportActivityByTimeResponse(
    @SerialName("activities")
    val activities: List<SportActivityResponse>
)