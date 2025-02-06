package ru.alexbur.backend.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import ru.alexbur.backend.di.BaseModule

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(BaseModule.json)
    }
}