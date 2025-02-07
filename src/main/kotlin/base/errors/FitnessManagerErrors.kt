package ru.alexbur.backend.base.errors

import ru.alexbur.backend.base.response.ErrorResponse
import ru.alexbur.backend.base.response.ErrorWrapperResponse
import ru.alexbur.backend.di.BaseModule

enum class FitnessManagerErrors(val code: String, val message: String) {
    BLOCKED_USER("BlockedUser", "Пользователь заблокирован, попробуйте авторизоваться позднее"),
    UNKNOWN_TOKEN("UnknownToken", "Неизвестный токен"),
    UNKNOWN_USER("UnknownUser", "Неизвестный пользователь"),
    BLOCK_USER("BlockUser", "Пользователь заблокирован на 1 час"),
    NO_SESSION_BY_USER("NoSessionByUser", "Данный пользователь ещё не получал одноразовый пароль для авторизации"),
    OLD_OTP("OldOtp", "Запросите новый одноразовый пароль по данному номеру телефона"),
    ERROR_OTP("ErrorOtp", "Неверный одноразовый пароль"),
    SESSION_NOT_FOUND("SessionNotFound", "У данного пользователя отсутствует сессия, необходимо авторизоваться заново"),
    ERROR_REFRESH_TOKEN("ErrorRefreshToken", "Необходимо авторизоваться заново")
}

fun createBadRequestError(error: FitnessManagerErrors): ErrorWrapperResponse {
    return ErrorWrapperResponse(
        error = BaseModule.json.encodeToJsonElement(
            ErrorResponse.serializer(),
            ErrorResponse(
                code = error.code,
                message = error.message
            )
        )
    )
}