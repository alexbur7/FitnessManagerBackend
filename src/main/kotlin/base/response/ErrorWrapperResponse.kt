package ru.alexbur.backend.base.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class ErrorWrapperResponse(
    val error: JsonElement
)