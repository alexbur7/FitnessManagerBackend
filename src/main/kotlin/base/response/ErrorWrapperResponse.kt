package ru.alexbur.backend.base.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class ErrorWrapperResponse(
    @SerialName("error")
    val error: JsonElement
)