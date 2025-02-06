package ru.alexbur.backend.base.response

import kotlinx.serialization.Serializable

@Serializable
class ErrorResponse(
    val code: String,
    val message: String,
)