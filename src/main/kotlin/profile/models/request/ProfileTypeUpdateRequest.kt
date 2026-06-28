package ru.alexbur.backend.profile.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileTypeUpdateRequest(
    @SerialName("is_trainer")
    val isTrainer: Boolean,
)
