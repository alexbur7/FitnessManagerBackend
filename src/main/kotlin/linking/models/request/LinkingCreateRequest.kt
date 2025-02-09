package ru.alexbur.backend.linking.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LinkingCreateRequest(
    @SerialName("client_card_id")
    val clientCardId: Long
)