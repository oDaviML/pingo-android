package com.pingo.core.notifications.di

import com.pingo.core.notifications.InstituicaoResolver
import com.pingo.core.notifications.PacoteInstituicaoResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CapturaProducaoModule {
    @Binds
    @Singleton
    abstract fun bindInstituicaoResolver(implementation: PacoteInstituicaoResolver): InstituicaoResolver
}
