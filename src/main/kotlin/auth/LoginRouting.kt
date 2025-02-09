package ru.alexbur.backend.auth

import auth.models.requests.GetOtpRequest
import auth.models.requests.LoginRequest
import auth.models.responses.GetOtpResponse
import auth.models.responses.LoginResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.alexbur.backend.auth.jwt.*
import ru.alexbur.backend.auth.routings.responses.RefreshTokenResponse
import ru.alexbur.backend.auth.service.AuthInfo
import ru.alexbur.backend.auth.service.AuthService
import ru.alexbur.backend.auth.service.SessionService
import ru.alexbur.backend.auth.service.UserService
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.base.utils.compareTimeWithCurrent
import ru.alexbur.backend.base.utils.getCurrentTimestamp
import ru.alexbur.backend.base.utils.getUserAgent
import java.security.SecureRandom

private const val DEFAULT_COUNT_LOGIN = 5
private const val BLOCKED_HOURS = 1L

fun Application.configureLoginRouting(
    jwtHelper: JwtHelper,
    userService: UserService,
    authService: AuthService,
    sessionService: SessionService
) {
    routing {
        post("/login/get-otp") {
            val request = call.receive<GetOtpRequest>()
            val userId = userService.create(request.phoneNumber)
            val session = authService.read(userId)

            if (session == null) {
                authService.create(createSession(userId))
            } else {
                if (session.blockedTime != null) {
                    if (compareTimeWithCurrent(session.blockedTime, BLOCKED_HOURS)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            createBadRequestError(FitnessManagerErrors.BLOCKED_USER)
                        )
                        return@post
                    }
                    authService.update(createSession(userId))
                }
            }
            call.respond(HttpStatusCode.OK, GetOtpResponse(userId).toSuccess(GetOtpResponse.serializer()))
        }

        post("/login/send-otp") {
            val request = call.receive<LoginRequest>()
            val user = userService.read(request.userId)
            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_USER))
                return@post
            }
            val session = authService.read(user.userId)
            if (session == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    createBadRequestError(FitnessManagerErrors.NO_SESSION_BY_USER)
                )
                return@post
            }
            if (session.blockedTime != null) {
                val error = if (compareTimeWithCurrent(session.blockedTime, BLOCKED_HOURS)) {
                    createBadRequestError(FitnessManagerErrors.BLOCKED_USER)
                } else {
                    createBadRequestError(FitnessManagerErrors.OLD_OTP)
                }
                call.respond(
                    HttpStatusCode.BadRequest,
                    error
                )
                return@post
            }
            if (session.otp != request.otp) {
                authService.update(
                    session.copy(
                        countLogin = session.countLogin.dec(),
                        blockedTime = if (session.countLogin == 1) getCurrentTimestamp() else null
                    )
                )
                val error = if (session.countLogin == 1) {
                    createBadRequestError(FitnessManagerErrors.BLOCK_USER)
                } else {
                    createBadRequestError(FitnessManagerErrors.ERROR_OTP)
                }
                call.respond(HttpStatusCode.BadRequest, error)
                return@post
            }
            authService.delete(user.userId)
            val accessToken = jwtHelper.generateAccessToken(user.userId)
            val refreshToken = jwtHelper.generateRefreshToken(user.userId)
            sessionService.create(user.userId, refreshToken, call.getUserAgent())

            call.respond(
                HttpStatusCode.OK, LoginResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                ).toSuccess(LoginResponse.serializer())
            )
        }

        post("/login/refresh") {
            val refreshToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            if (refreshToken == null) {
                call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_TOKEN))
                return@post
            }
            val userId = jwtHelper.verifyToken(refreshToken)
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_USER))
                return@post
            }
            val session = sessionService.read(userId, call.getUserAgent())
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, createBadRequestError(FitnessManagerErrors.SESSION_NOT_FOUND))
                return@post
            }

            if (session.refreshToken != refreshToken) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    createBadRequestError(FitnessManagerErrors.ERROR_REFRESH_TOKEN)
                )
                return@post
            }

            val accessToken = jwtHelper.generateAccessToken(session.userId)

            if (jwtHelper.isTokenExpired(refreshToken)) {
                val refreshToken = jwtHelper.generateRefreshToken(session.userId)
                sessionService.update(refreshToken, session.id)
                call.respond(
                    HttpStatusCode.OK, RefreshTokenResponse(
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    ).toSuccess(RefreshTokenResponse.serializer())
                )
                return@post
            }

            // если refreshToken не истек, то не генерируем его заново и не отправляем клиенту
            call.respond(
                HttpStatusCode.OK, RefreshTokenResponse(
                    accessToken = accessToken,
                    refreshToken = null
                ).toSuccess(RefreshTokenResponse.serializer())
            )
        }
    }
}

private fun createSession(userId: Long): AuthInfo {
    return AuthInfo(
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
