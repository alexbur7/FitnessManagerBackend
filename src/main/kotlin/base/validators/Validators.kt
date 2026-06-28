package ru.alexbur.backend.base.validators

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import ru.alexbur.backend.auth.models.requests.GetOtpRequest
import ru.alexbur.backend.linking.LINKING_CODE_LENGTH
import ru.alexbur.backend.linking.models.request.LinkingConnectRequest
import ru.alexbur.backend.linking.models.request.LinkingCreateRequest

private val PHONE_REGEX = Regex("^7\\d{10}$")

fun Application.setupValidators() {
    install(RequestValidation) {
        validate<GetOtpRequest> { request ->
            if (PHONE_REGEX.matches(request.phoneNumber)) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("Номер телефона должен начинаться с 7 и содержать 11 цифр.")
            }
        }

        validate<LinkingCreateRequest> { request ->
            if (request.clientId > 0) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("ClientId must be greater than 0.")
            }
        }

        validate<LinkingConnectRequest> { request ->
            if (request.code.length == LINKING_CODE_LENGTH) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("Code must be length 8 symbol.")
            }
        }
    }
}