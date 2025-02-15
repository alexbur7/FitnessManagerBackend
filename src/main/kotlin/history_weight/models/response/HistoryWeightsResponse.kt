package ru.alexbur.backend.history_weight.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryWeightsResponse(
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("weights")
    val weights: List<HistoryWeightResponse>,
)