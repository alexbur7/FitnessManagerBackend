package ru.alexbur.backend.events.models.response

import base.serializers.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
class EventResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("start_time")
    @Serializable(with = TimestampSerializer::class)
    val startTime: Timestamp,
    @SerialName("end_time")
    @Serializable(with = TimestampSerializer::class)
    val endTime: Timestamp,
    @SerialName("is_cancelled")
    val isCancelled: Boolean,
    @SerialName("comment")
    val comment: String?,
    @SerialName("relationship_id")
    val relationshipId: Long,
)