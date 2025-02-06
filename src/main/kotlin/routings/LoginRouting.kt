package ru.alexbur.backend.routings

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import routings.requests.GetOtpRequest
import routings.requests.LoginRequest
import routings.responses.GetOtpResponse
import routings.responses.LoginResponse
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadGatewayError
import ru.alexbur.backend.di.BaseModule
import ru.alexbur.backend.plugins.createJWT
import ru.alexbur.backend.service.Session
import ru.alexbur.backend.service.SessionService
import ru.alexbur.backend.service.UserService
import ru.alexbur.backend.utils.compareTimeWithCurrent
import ru.alexbur.backend.utils.getCurrentTimestamp
import java.security.SecureRandom
import java.sql.Connection

private const val DEFAULT_COUNT_LOGIN = 5
private const val BLOCKED_HOURS = 1L

fun Application.configureLoginRouting(dbConnection: Connection) {
    val userService = UserService(dbConnection, BaseModule.dispatcherProvider)
    val sessionService = SessionService(dbConnection, BaseModule.dispatcherProvider)
    routing {
        post("/login/get-otp") {
            val request = call.receive<GetOtpRequest>()
            val userId = userService.create(request.phoneNumber)
            val session = sessionService.read(userId)

            if (session == null) {
                sessionService.create(createSession(userId))
            } else {
                if (session.blockedTime != null) {
                    if (compareTimeWithCurrent(session.blockedTime, BLOCKED_HOURS)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            createBadGatewayError(FitnessManagerErrors.BLOCKED_USER)
                        )
                        return@post
                    }
                    sessionService.update(createSession(userId))
                }
            }
            call.respond(HttpStatusCode.OK, GetOtpResponse(userId))
        }

        post("/login/send-otp") {
            val request = call.receive<LoginRequest>()
            val user = userService.read(request.userId)
            if (user == null) {
                call.respond(HttpStatusCode.BadRequest,createBadGatewayError(FitnessManagerErrors.UNKNOWN_USER))
                return@post
            }
            val session = sessionService.read(user.userId)
            if (session == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    createBadGatewayError(FitnessManagerErrors.NO_SESSION_BY_USER)
                )
                return@post
            }
            if (session.blockedTime != null) {
                val error = if (compareTimeWithCurrent(session.blockedTime, BLOCKED_HOURS)) {
                    createBadGatewayError(FitnessManagerErrors.BLOCKED_USER)
                } else {
                    createBadGatewayError(FitnessManagerErrors.OLD_OTP)
                }
                call.respond(
                    HttpStatusCode.BadRequest,
                    error
                )
                return@post
            }
            if (session.otp != request.otp) {
                sessionService.update(
                    session.copy(
                        countLogin = session.countLogin.dec(),
                        blockedTime = if (session.countLogin == 1) getCurrentTimestamp() else null
                    )
                )
                val error = if (session.countLogin == 1) {
                    createBadGatewayError(FitnessManagerErrors.BLOCK_USER)
                } else {
                    createBadGatewayError(FitnessManagerErrors.ERROR_OTP)
                }
                call.respond(HttpStatusCode.BadRequest, error)
                return@post
            }
            sessionService.delete(user.userId)
            call.respond(HttpStatusCode.OK, LoginResponse(createJWT(user.userId)))
        }
    }
}

private fun createSession(userId: Long): Session {
    return Session(
        userId = userId,
        otp = generateOtp(),
        countLogin = DEFAULT_COUNT_LOGIN,
        blockedTime = null
    )
}

private fun generateOtp(): String {
    val otp = StringBuilder()
    val digits = "0123456789"

    repeat(6) {
        val randomIndex = SecureRandom().nextInt(digits.length)
        otp.append(digits[randomIndex])
    }

    return otp.toString()
}
