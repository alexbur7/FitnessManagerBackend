package ru.alexbur.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import ru.alexbur.backend.auth.configureLoginRouting
import ru.alexbur.backend.auth.configureSecurity
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.client_card.configureClientCardRouting
import ru.alexbur.backend.di.BaseModule
import ru.alexbur.backend.di.MappersModule
import ru.alexbur.backend.plugins.configureMonitoring
import ru.alexbur.backend.plugins.configureSerialization
import ru.alexbur.backend.sport_activity.configureSportActivityRouting

fun main(args: Array<String>) {
    embeddedServer(
        Netty,
        port = 8080,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                createBadRequestError("ValidationError", cause.reasons.joinToString())
            )
        }
    }

    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureLoginRouting(BaseModule.provideJwtGenerator(this))
    configureSportActivityRouting(MappersModule.provideSportActivityMapper())
    configureClientCardRouting()
}