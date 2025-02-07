package ru.alexbur.backend.sport_activity.mapper

import ru.alexbur.backend.sport_activity.models.response.SportActivityResponse
import ru.alexbur.backend.sport_activity.service.SportActivity

class SportActivityMapper {

    fun map(model: SportActivity) = SportActivityResponse(
        id = model.id,
        userId = model.userId,
        name = model.name,
        startTime = model.startTime,
        endTime = model.endTime,
        isEnded = model.isEnded,
        comment = model.comment,
        clientCardId = model.clientCardId
    )
}