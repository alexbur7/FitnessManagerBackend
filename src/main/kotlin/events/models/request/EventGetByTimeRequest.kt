package ru.alexbur.backend.events.models.request

import base.serializers.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
data class EventGetByTimeRequest(
    @SerialName("start_time")
    @Serializable(with = TimestampSerializer::class)
    val startTime: Timestamp,
    @SerialName("end_time")
    @Serializable(with = TimestampSerializer::class)
    val endTime: Timestamp,
)