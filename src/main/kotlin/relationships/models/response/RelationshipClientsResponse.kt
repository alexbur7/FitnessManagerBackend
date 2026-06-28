package ru.alexbur.backend.relationships.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RelationshipClientsResponse(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("ids") val ids: List<Long>,
)
