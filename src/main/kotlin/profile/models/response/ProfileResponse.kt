package ru.alexbur.backend.profile.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    @SerialName("user_id")
    val userId: Long,
    @SerialName("email")
    val email: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("second_name")
    val secondName: String? = null,
    @SerialName("date_birthday")
    val dateBirthday: String? = null,
    @SerialName("is_trainer")
    val isTrainer: Boolean,
)
