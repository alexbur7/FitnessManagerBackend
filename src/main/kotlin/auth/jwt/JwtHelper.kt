package ru.alexbur.backend.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.*
import ru.alexbur.backend.auth.jwt.JwtHelper.Companion.ACCESS_TOKEN_LIFETIME
import ru.alexbur.backend.auth.jwt.JwtHelper.Companion.REFRESH_TOKEN_LIFETIME
import ru.alexbur.backend.auth.jwt.JwtHelper.Companion.USER_ID_KEY
import ru.alexbur.backend.base.utils.getCurrentTimestamp
import java.util.*

class JwtHelper(
    private val application: Application
) {

    fun decodeToken(token: String): DecodedJWT? {
        val secret = application.environment.config.property("jwt.secret").getString()
        val issuer = application.environment.config.property("jwt.issuer").getString()
        return try {
            JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (_: Exception) {
            null // Токен недействителен
        }
    }

    fun generateToken(userId: Long, lifeTime: Long): String {
        val secret = application.environment.config.property("jwt.secret").getString()
        val issuer = application.environment.config.property("jwt.issuer").getString()
        val audience = application.environment.config.property("jwt.audience").getString()
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(USER_ID_KEY, userId)
            .withExpiresAt(Date(getCurrentTimestamp().time + lifeTime))
            .sign(Algorithm.HMAC256(secret))
    }

    internal companion object {
        const val ACCESS_TOKEN_LIFETIME = 1000 * 60 * 60 * 15L // 15 минут
        const val REFRESH_TOKEN_LIFETIME = 1000 * 60 * 60 * 24 * 30L // 30 дней
        const val USER_ID_KEY = "user_id"
    }
}

fun JwtHelper.verifyToken(token: String): Long? {
    return decodeToken(token)?.claims[USER_ID_KEY]?.asLong()
}

fun JwtHelper.isTokenExpired(token: String): Boolean {
    return decodeToken(token)?.expiresAt?.before(Date()) != false
}

fun JwtHelper.generateRefreshToken(userId: Long): String {
    return generateToken(userId, REFRESH_TOKEN_LIFETIME)
}

fun JwtHelper.generateAccessToken(userId: Long): String {
    return generateToken(userId, ACCESS_TOKEN_LIFETIME)
}