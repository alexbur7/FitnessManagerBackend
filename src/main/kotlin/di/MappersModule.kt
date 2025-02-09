package ru.alexbur.backend.di

import ru.alexbur.backend.client_card.mapper.ClientCardMapper
import ru.alexbur.backend.events.mapper.EventMapper

object MappersModule {

    fun provideSportActivityMapper(): EventMapper {
        return EventMapper()
    }

    fun provideClientCardMapper(): ClientCardMapper {
        return ClientCardMapper()
    }
}