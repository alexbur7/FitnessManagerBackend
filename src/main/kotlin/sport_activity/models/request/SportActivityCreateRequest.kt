package ru.alexbur.backend.sport_activity.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SportActivityCreateRequest(
    @SerialName("name")
    val name: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("comment")
    val comment: String?,
    @SerialName("client_card_id")
    val clientCardId: Long,
)