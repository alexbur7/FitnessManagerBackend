package ru.alexbur.backend.di

import io.ktor.server.application.Application
import kotlinx.serialization.json.Json
import ru.alexbur.backend.auth.jwt.JwtHelper
import ru.alexbur.backend.utils.DispatcherProvider

object BaseModule {
    val dispatcherProvider: DispatcherProvider = DispatcherProvider()
    val json = Json {
        ignoreUnknownKeys = true
    }

    fun provideJwtGenerator(application: Application): JwtHelper {
        return JwtHelper(application)
    }
}