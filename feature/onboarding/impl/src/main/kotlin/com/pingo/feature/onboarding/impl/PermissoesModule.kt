package com.pingo.feature.onboarding.impl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissoesModule {
    @Binds
    @Singleton
    abstract fun bindFabricanteDoAparelho(implementation: FabricanteDoAparelhoDoAndroid): FabricanteDoAparelho

    @Binds
    @Singleton
    abstract fun bindBateriaDoAparelho(implementation: BateriaDoAparelhoDoAndroid): BateriaDoAparelho
}
