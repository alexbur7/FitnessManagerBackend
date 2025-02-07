package ru.alexbur.backend.sport_activity.models.request

import base.serializers.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
class SportActivityCreateRequest(
    @SerialName("name")
    val name: String,
    @SerialName("start_time")
    @Serializable(with = TimestampSerializer::class)
    val startTime: Timestamp,
    @SerialName("end_time")
    @Serializable(with = TimestampSerializer::class)
    val endTime: Timestamp,
    @SerialName("comment")
    val comment: String?,
    @SerialName("client_card_id")
    val clientCardId: Long,
)