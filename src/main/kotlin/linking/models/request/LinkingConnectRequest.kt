package ru.alexbur.backend.linking.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkingConnectRequest(
    @SerialName("code")
    val code: String
)