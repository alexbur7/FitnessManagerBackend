package ru.alexbur.backend.di

import ru.alexbur.backend.events.mapper.EventMapper
import ru.alexbur.backend.history_weight.mappers.HistoryWeightMapper

object MappersModule {

    fun provideSportActivityMapper(): EventMapper {
        return EventMapper()
    }

    fun provideHistoryWeightMapper(): HistoryWeightMapper {
        return HistoryWeightMapper()
    }
}
