package ru.alexbur.backend.sport_activity.mapper

import ru.alexbur.backend.sport_activity.models.request.SportActivityCreateRequest
import ru.alexbur.backend.sport_activity.models.response.SportActivityResponse
import ru.alexbur.backend.sport_activity.service.SportActivity
import ru.alexbur.backend.sport_activity.service.SportActivityCreate

class SportActivityMapper {

    fun map(model: SportActivity) = SportActivityResponse(
        id = model.id,
        userId = model.userId,
        startTime = model.startTime,
        endTime = model.endTime,
        isEnded = model.isEnded,
        comment = model.comment,
        clientCardId = model.clientCardId,
    )

    fun map(model: SportActivityCreateRequest, userId: Long) = SportActivityCreate(
        userId = userId,
        startTime = model.startTime,
        endTime = model.endTime,
        comment = model.comment,
        clientCardId = model.clientCardId,
        isEnded = model.isEnded,
    )
}