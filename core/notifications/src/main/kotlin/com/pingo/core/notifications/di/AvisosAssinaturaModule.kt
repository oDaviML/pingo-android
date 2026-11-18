package com.pingo.core.notifications.di

import com.pingo.core.data.assinatura.AgendadorDeAvisosDeAssinatura
import com.pingo.core.notifications.NotificadorDeAssinatura
import com.pingo.core.notifications.NotificadorDeAssinaturaDoAndroid
import com.pingo.core.notifications.WorkManagerAgendadorDeAvisosDeAssinatura
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AvisosAssinaturaModule {
    @Binds
    @Singleton
    abstract fun bindAgendadorDeAvisos(implementation: WorkManagerAgendadorDeAvisosDeAssinatura): AgendadorDeAvisosDeAssinatura

    @Binds
    @Singleton
    abstract fun bindNotificadorDeAssinatura(implementation: NotificadorDeAssinaturaDoAndroid): NotificadorDeAssinatura
}
