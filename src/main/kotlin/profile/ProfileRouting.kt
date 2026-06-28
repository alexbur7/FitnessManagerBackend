package ru.alexbur.backend.profile

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.utils.getUserId
import ru.alexbur.backend.profile.data.ProfileType
import ru.alexbur.backend.profile.models.request.ProfileTypeUpdateRequest
import ru.alexbur.backend.profile.service.ProfileService
import ru.alexbur.backend.profile.service.ProfileTypeUpdate

internal fun Application.configureProfileRouting(service: ProfileService) {
    routing {
        authenticate("auth-jwt") {
            put("/profile/type") {
                val userId = call.getUserId() ?: return@put
                val request = call.receive<ProfileTypeUpdateRequest>()
                try {
                    val updatedRows = service.typeUpdate(
                        userId = userId,
                        data = ProfileTypeUpdate(
                            profileType = if (request.isTrainer) ProfileType.COACH else ProfileType.CLIENT
                        )
                    )
                    if (updatedRows > 0) {
                        call.respond(HttpStatusCode.OK)
                        return@put
                    }
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.ERROR_UPDATE)
                    )
                } catch (_: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.UNKNOWN_ERROR)
                    )
                }
            }
        }
    }
}
