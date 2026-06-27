package ru.alexbur.backend.base.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    fun io(): CoroutineDispatcher
    fun default(): CoroutineDispatcher

    companion object {
        operator fun invoke(): DispatcherProvider {
            return DispatcherProviderImpl()
        }
    }
}

private class DispatcherProviderImpl : DispatcherProvider {
    override fun io(): CoroutineDispatcher = Dispatchers.IO
    override fun default(): CoroutineDispatcher = Dispatchers.Default
}