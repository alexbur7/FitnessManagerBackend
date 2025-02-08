package ru.alexbur.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.alexbur.backend.auth.configureLoginRouting
import ru.alexbur.backend.auth.configureSecurity
import ru.alexbur.backend.client_card.configureClientCardRouting
import ru.alexbur.backend.db.getConnection
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
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureLoginRouting(BaseModule.provideJwtGenerator(this))
    configureSportActivityRouting(MappersModule.provideSportActivityMapper())
    configureClientCardRouting()
}