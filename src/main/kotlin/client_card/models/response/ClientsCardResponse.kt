package ru.alexbur.backend.client_card.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientsCardResponse(
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("clients")
    val clients: List<ClientCardResponse>
)