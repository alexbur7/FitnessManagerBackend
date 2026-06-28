package ru.alexbur.backend.profile

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.base.utils.getUserId
import ru.alexbur.backend.profile.models.request.ProfileUpdateRequest
import ru.alexbur.backend.profile.models.response.ProfileResponse
import ru.alexbur.backend.profile.service.ProfileService

internal fun Application.configureProfileRouting(service: ProfileService) {
    routing {
        authenticate("auth-jwt") {
            put("/profile") {
                val userId = call.getUserId() ?: return@put
                val request = call.receive<ProfileUpdateRequest>()
                val response = service.upsert(userId = userId, request = request)
                if (response == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.ERROR_UPDATE)
                    )
                    return@put
                }
                call.respond(HttpStatusCode.OK, response.toSuccess(ProfileResponse.serializer()))
            }
        }
    }
}
