package ru.alexbur.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.alexbur.backend.auth.configureLoginRouting
import ru.alexbur.backend.auth.configureSecurity
import ru.alexbur.backend.db.connectToPostgres
import ru.alexbur.backend.di.BaseModule
import ru.alexbur.backend.di.MappersModule
import ru.alexbur.backend.plugins.configureMonitoring
import ru.alexbur.backend.plugins.configureSerialization
import ru.alexbur.backend.sport_activity.configureCalendarRouting

fun main(args: Array<String>) {
    embeddedServer(
        Netty,
        port = 8080,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val dbConnection = connectToPostgres(embedded = false)
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureLoginRouting(dbConnection, BaseModule.provideJwtGenerator(this))
    configureCalendarRouting(dbConnection, MappersModule.provideSportActivityMapper())
}