package ru.alexbur.backend.sport_activity.models.response

import base.serializers.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
class SportActivityResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("user_id")
    val userId: Long,
    @SerialName("start_time")
    @Serializable(with = TimestampSerializer::class)
    val startTime: Timestamp,
    @SerialName("end_time")
    @Serializable(with = TimestampSerializer::class)
    val endTime: Timestamp,
    @SerialName("is_ended")
    val isEnded: Boolean,
    @SerialName("comment")
    val comment: String?,
    @SerialName("client_card_id")
    val clientCardId: Long,
)