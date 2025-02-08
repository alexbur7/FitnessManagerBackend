package ru.alexbur.backend.client_card.mapper

import ru.alexbur.backend.client_card.models.request.ClientCardCreateRequest
import ru.alexbur.backend.client_card.models.response.ClientCardFullResponse
import ru.alexbur.backend.client_card.models.response.ClientCardResponse
import ru.alexbur.backend.client_card.models.response.ClientsCardResponse
import ru.alexbur.backend.client_card.service.ClientCard
import ru.alexbur.backend.client_card.service.ClientCardCreate
import ru.alexbur.backend.client_card.service.ClientCardFull
import ru.alexbur.backend.client_card.service.ClientsCard

class ClientCardMapper {

    fun map(request: ClientCardCreateRequest, coachId: Long) = ClientCardCreate(
        name = request.name,
        age = request.age,
        weightGm = request.weightGm,
        phone = request.phone,
        coachId = coachId
    )

    fun map(model: ClientCardFull) = ClientCardFullResponse(
        id = model.id,
        name = model.name,
        photoUrl = model.photoUrl,
        age = model.age,
        weightGm = model.weightGm,
        phone = model.phone
    )

    fun map(model: ClientsCard) = ClientsCardResponse(
        totalCount = model.totalCount,
        clients = model.clients.map { client -> map(client) }
    )

    private fun map(model: ClientCard) = ClientCardResponse(
        id = model.id,
        name = model.name,
        photoUrl = model.photoUrl
    )
}