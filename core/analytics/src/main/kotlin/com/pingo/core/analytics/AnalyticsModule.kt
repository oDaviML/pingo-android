package com.pingo.core.analytics

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    internal abstract fun bindCanalTelemetria(impl: CanalFirebaseAndroid): CanalTelemetria

    @Binds
    @Singleton
    abstract fun bindAnalyticsFacade(impl: FirebaseAnalyticsFacade): AnalyticsFacade
}
