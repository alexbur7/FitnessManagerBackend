package ru.alexbur.backend.utils

import io.ktor.server.routing.*

fun RoutingRequest.getUserAgent(): String {
    return headers["user-agent"] ?: "DefaultAgent"
}