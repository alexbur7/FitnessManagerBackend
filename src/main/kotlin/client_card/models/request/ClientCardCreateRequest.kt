package ru.alexbur.backend.client_card.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientCardCreateRequest(
    @SerialName("name")
    val name: String,
    @SerialName("age")
    val age: Int?,
    @SerialName("weight_gm")
    val weightGm: Int?,
    @SerialName("phone")
    val phone: String?,
)