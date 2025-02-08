package ru.alexbur.backend.base.utils

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.alexbur.backend.auth.jwt.JwtHelper.Companion.USER_ID_KEY
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError

fun RoutingCall.getUserAgent(): String {
    return request.headers["user-agent"] ?: "DefaultAgent"
}

suspend fun RoutingCall.getUserId(): Long? {
    val principal = authentication.principal<JWTPrincipal>()
    val userId = principal?.payload?.getClaim(USER_ID_KEY)?.asLong()
    if (userId == null) {
        respond(HttpStatusCode.Unauthorized, createBadRequestError(FitnessManagerErrors.UNKNOWN_USER))
    }
    return userId
}