package ru.alexbur.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.alexbur.backend.auth.configureSecurity
import ru.alexbur.backend.auth.routings.configureLoginRouting
import ru.alexbur.backend.auth.routings.configureRouting
import ru.alexbur.backend.db.connectToPostgres
import ru.alexbur.backend.di.BaseModule
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
    val dbConnection = connectToPostgres(embedded = false)
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureRouting()
    configureLoginRouting(dbConnection, BaseModule.provideJwtGenerator(this))
}
