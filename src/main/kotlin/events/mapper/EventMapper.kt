package ru.alexbur.backend.events.mapper

import ru.alexbur.backend.events.models.request.EventCreateRequest
import ru.alexbur.backend.events.models.response.EventResponse
import ru.alexbur.backend.events.service.Event
import ru.alexbur.backend.events.service.EventCreate

class EventMapper {

    fun map(model: Event) = EventResponse(
        id = model.id,
        startTime = model.startTime,
        endTime = model.endTime,
        isCancelled = model.isCancelled,
        comment = model.comment,
        relationshipId = model.relationshipId,
    )

    fun map(model: EventCreateRequest) = EventCreate(
        startTime = model.startTime,
        endTime = model.endTime,
        comment = model.comment,
        relationshipId = model.relationshipId,
        isCancelled = model.isCancelled,
    )
}