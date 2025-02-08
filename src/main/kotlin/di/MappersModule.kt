package ru.alexbur.backend.di

import ru.alexbur.backend.client_card.mapper.ClientCardMapper
import ru.alexbur.backend.sport_activity.mapper.SportActivityMapper

object MappersModule {

    fun provideSportActivityMapper(): SportActivityMapper {
        return SportActivityMapper()
    }

    fun provideClientCardMapper(): ClientCardMapper {
        return ClientCardMapper()
    }
}