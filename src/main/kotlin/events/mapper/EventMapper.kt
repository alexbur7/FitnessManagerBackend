package ru.alexbur.backend.events.mapper

import ru.alexbur.backend.events.models.request.EventCreateRequest
import ru.alexbur.backend.events.models.response.EventResponse
import ru.alexbur.backend.events.service.Event
import ru.alexbur.backend.events.service.EventCreate

class EventMapper {

    fun map(model: Event) = EventResponse(
        id = model.id,
        userId = model.userId,
        startTime = model.startTime,
        endTime = model.endTime,
        isEnded = model.isEnded,
        comment = model.comment,
        clientCardId = model.clientCardId,
    )

    fun map(model: EventCreateRequest, userId: Long) = EventCreate(
        userId = userId,
        startTime = model.startTime,
        endTime = model.endTime,
        comment = model.comment,
        clientCardId = model.clientCardId,
        isEnded = model.isEnded,
    )
}