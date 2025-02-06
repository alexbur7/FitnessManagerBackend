package ru.alexbur.backend.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    fun io(): CoroutineDispatcher
    fun main(): CoroutineDispatcher
    fun default(): CoroutineDispatcher

    companion object {
        operator fun invoke(): DispatcherProvider {
            return DispatcherProviderImpl()
        }
    }
}

private class DispatcherProviderImpl() : DispatcherProvider {
    override fun io(): CoroutineDispatcher = Dispatchers.IO
    override fun main(): CoroutineDispatcher = Dispatchers.Main.immediate
    override fun default(): CoroutineDispatcher = Dispatchers.Default
}