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
import ru.alexbur.backend.db.getConnection
import ru.alexbur.backend.db.initConnectionPool
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
import ru.alexbur.backend.profile.configureProfileRouting
import ru.alexbur.backend.profile.service.ProfileService
import relationships.configureRelationshipsRouting
import ru.alexbur.backend.relationships.service.RelationshipsService

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

    initConnectionPool()

    val historyWeightMapper = MappersModule.provideHistoryWeightMapper()

    val relationshipsService = RelationshipsService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val linkingService = LinkingService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val userService = UserService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val eventService = EventService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val authService = AuthService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val sessionService = SessionService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val historyWeightService = HistoryWeightService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val profileService = ProfileService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    setupValidators()
    configureLoginRouting(BaseModule.provideJwtGenerator(this), userService, authService, sessionService)
    configureEventRouting(MappersModule.provideSportActivityMapper(), relationshipsService, eventService)
    configureRelationshipsRouting(relationshipsService, profileService)
    configureLinkingRouting(linkingService, relationshipsService, userService, BaseModule.dispatcherProvider)
    configureHistoryWeightRouting(historyWeightService, relationshipsService, historyWeightMapper)
    configureProfileRouting(profileService)
}
