package ru.alexbur.backend.events.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EventByTimeResponse(
    @SerialName("activities")
    val activities: List<EventResponse>
)