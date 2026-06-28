package ru.alexbur.backend.relationships.models.response

import kotlinx.serialization.Serializable

@Serializable
internal data class RelationshipIdsResponse(val ids: List<Long>)
