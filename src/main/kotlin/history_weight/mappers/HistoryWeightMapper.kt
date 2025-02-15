package ru.alexbur.backend.history_weight.mappers

import ru.alexbur.backend.history_weight.models.response.HistoryWeightResponse
import ru.alexbur.backend.history_weight.models.response.HistoryWeightsResponse
import ru.alexbur.backend.history_weight.service.HistoryWeight
import ru.alexbur.backend.history_weight.service.HistoryWeights

class HistoryWeightMapper {

    fun map(data: HistoryWeight) = HistoryWeightResponse(
        id = data.id,
        weightGm = data.weightGm,
        date = data.date
    )

    fun map(data: HistoryWeights) = HistoryWeightsResponse(
        totalCount = data.totalCount,
        weights = data.weights.map { map(it) }
    )
}