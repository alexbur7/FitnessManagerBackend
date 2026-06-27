package ru.alexbur.backend.auth.models.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RefreshTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String?,
)