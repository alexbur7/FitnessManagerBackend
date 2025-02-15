package ru.alexbur.backend.history_weight.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryWeightUpdateRequest(
    @SerialName("weight_gm")
    val weightGm: Int
)