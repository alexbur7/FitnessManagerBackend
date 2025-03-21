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
    ERROR_REFRESH_TOKEN("ErrorRefreshToken", "Необходимо авторизоваться заново"),
    UNKNOWN_ID("UnknownId", "Идентификатор не отправлен"),
    UNKNOWN_EVENT("UnknownSportActivity", "Тренировка с таким идентификатором не найдена"),
    UNKNOWN_CLIENT_CARD("UnknownClientCard", "Карточка клиента с таким идентификатором не найдена"),
    ERROR_CREATE_SPORT_ACTIVITY("UnknownSportActivity", "У вас уже есть тренировка с данным клиентом в этом время"),
    INVALID_END_TIME("InvalidEndTime", "Дата окончания не может быть раньше даты начала"),
    ERROR_UPDATE("ErrorUpdate", "Ошибка обновления данных"),
    ERROR_DELETE("ErrorDelete", "Ошибка удаления данных"),
    UNKNOWN_LINKING_CODE("UnknownLinkingCode", "Неизвестный код для связи тренера и клиента"),
    EXPIRED_LINKING_CODE(
        "ExpiredLinkingCode", "У вашего кода истекло время использования. Попросите тренера сделать новый"
    ),
    UNKNOWN_HISTORY_WEIGHT("UnknownHistoryWeight", "Неизвестный идентификатор веса"),
    EDITING_IS_PROHIBITED("EditingIsProhibited", "Историю веса запрещено редактировать всем, кроме тренера"),
    DONT_EDITING_WEIGHT("DontEditingWeight", "Вес не удалось изменить"),
    DONT_DELETED_WEIGHT("DontDeletedWeight", "Вес не удалось удалить"),
}

fun createBadRequestError(error: FitnessManagerErrors): ErrorWrapperResponse {
    return createBadRequestError(error.code, error.message)
}

fun createBadRequestError(code: String, message: String): ErrorWrapperResponse {
    return ErrorWrapperResponse(
        error = BaseModule.json.encodeToJsonElement(
            ErrorResponse.serializer(),
            ErrorResponse(
                code = code,
                message = message
            )
        )
    )
}