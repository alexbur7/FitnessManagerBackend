package ru.alexbur.backend.base.success

import kotlinx.serialization.SerializationStrategy
import ru.alexbur.backend.base.response.SuccessResponse
import ru.alexbur.backend.di.BaseModule

fun <T> T.toSuccess(serializer: SerializationStrategy<T>): SuccessResponse {
    return SuccessResponse(
        BaseModule.json.encodeToJsonElement(
            serializer,
            this
        )
    )
}