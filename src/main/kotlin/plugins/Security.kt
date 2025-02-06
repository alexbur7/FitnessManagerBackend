package ru.alexbur.backend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

private const val TOKEN_LIFETIME = 1000 * 60 * 15 // 15 минут

/**
 * Настройка работы с авторизацией через jwt
 */
fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain
    val jwtAudience = "jwt-audience"
    val jwtRealm = "ktor sample app"
    val jwtSecret = "secret"
    val issuer = "issuer"
    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

fun createJWT(userId: Long): String {
    val secret = "secret"
    val issuer = "issuer"
    val audience = "jwt-audience"
    return JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("user_id", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_LIFETIME))
        .sign(Algorithm.HMAC256(secret))
}
