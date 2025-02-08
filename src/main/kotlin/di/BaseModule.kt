package ru.alexbur.backend.di

import io.ktor.server.application.Application
import kotlinx.serialization.json.Json
import ru.alexbur.backend.auth.jwt.JwtHelper
import ru.alexbur.backend.base.utils.DispatcherProvider

object BaseModule {
    val dispatcherProvider: DispatcherProvider = DispatcherProvider()
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun provideJwtGenerator(application: Application): JwtHelper {
        return JwtHelper(application)
    }
}