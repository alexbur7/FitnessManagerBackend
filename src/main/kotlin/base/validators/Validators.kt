package ru.alexbur.backend.base.validators

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import ru.alexbur.backend.client_card.models.request.ClientCardCreateRequest
import ru.alexbur.backend.linking.LINKING_CODE_LENGTH
import ru.alexbur.backend.linking.models.request.LinkingConnectRequest
import ru.alexbur.backend.linking.models.request.LinkingCreateRequest

fun Application.setupValidators() {
    install(RequestValidation) {
        validate<ClientCardCreateRequest> { request ->
            val errorMessage = mutableListOf<String>()
            if (request.weightGm != null && request.weightGm <= 0) {
                errorMessage.add("Weight  must be greater than 0.")
            }

            if (request.age != null && request.age <= 0) {
                errorMessage.add("Age must be greater than 0.")
            }
            if (errorMessage.isEmpty()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(errorMessage.joinToString(separator = " "))
            }
        }

        validate<LinkingCreateRequest> { request ->
            if (request.clientCardId > 0) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("ClientCardId must be greater than 0.")
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