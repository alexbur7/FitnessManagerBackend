package ru.alexbur.backend.di

import kotlinx.serialization.json.Json
import ru.alexbur.backend.utils.DispatcherProvider

object BaseModule {
    val dispatcherProvider: DispatcherProvider = DispatcherProvider()
    val json = Json {
        ignoreUnknownKeys = true
    }
}