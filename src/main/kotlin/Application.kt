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
import ru.alexbur.backend.auth.service.AuthService
import ru.alexbur.backend.auth.service.SessionService
import ru.alexbur.backend.auth.service.UserService
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.validators.setupValidators
import ru.alexbur.backend.client_card.configureClientCardRouting
import ru.alexbur.backend.client_card.service.ClientsCardService
import ru.alexbur.backend.db.getConnection
import ru.alexbur.backend.di.BaseModule
import ru.alexbur.backend.di.MappersModule
import ru.alexbur.backend.events.configureEventRouting
import ru.alexbur.backend.events.service.EventService
import ru.alexbur.backend.history_weight.configureHistoryWeightRouting
import ru.alexbur.backend.history_weight.service.HistoryWeightService
import ru.alexbur.backend.linking.configureLinkingRouting
import ru.alexbur.backend.linking.service.LinkingService
import ru.alexbur.backend.plugins.configureMonitoring
import ru.alexbur.backend.plugins.configureSerialization

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

    val mapper = MappersModule.provideClientCardMapper()
    val historyWeightMapper = MappersModule.provideHistoryWeightMapper()

    val clientCardService = ClientsCardService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val linkingService = LinkingService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val userService = UserService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val eventService = EventService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val authService = AuthService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val sessionService = SessionService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val historyWeightService = HistoryWeightService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    setupValidators()
    configureLoginRouting(BaseModule.provideJwtGenerator(this), userService, authService, sessionService)
    configureEventRouting(MappersModule.provideSportActivityMapper(), clientCardService, eventService)
    configureClientCardRouting(clientCardService, mapper, historyWeightService, BaseModule.dispatcherProvider)
    configureLinkingRouting(linkingService, clientCardService, userService, BaseModule.dispatcherProvider)
    configureHistoryWeightRouting(historyWeightService, clientCardService, historyWeightMapper)
}