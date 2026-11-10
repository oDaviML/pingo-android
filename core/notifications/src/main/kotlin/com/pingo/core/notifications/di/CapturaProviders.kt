package com.pingo.core.notifications.di

import com.pingo.core.parser.DefaultParserEngine
import com.pingo.core.parser.ParserEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CapturaProviders {
    @Provides
    @Singleton
    fun provideParserEngine(): ParserEngine = DefaultParserEngine()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
