package ru.alexbur.backend.history_weight.models.response

import base.serializers.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
data class HistoryWeightResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("weight_gm")
    val weightGm: Int,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("date")
    val date: Timestamp,
)