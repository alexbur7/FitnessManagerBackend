package ru.alexbur.backend.linking.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkingInfoResponse(
    @SerialName("code")
    val code: String
)