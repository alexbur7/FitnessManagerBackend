package ru.alexbur.backend.client_card.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientCardFullResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("photo_url")
    val photoUrl: String?,
    @SerialName("age")
    val age: Int?,
    @SerialName("weight_gm")
    val weightGm: Int?,
    @SerialName("phone")
    val phone: String?
)