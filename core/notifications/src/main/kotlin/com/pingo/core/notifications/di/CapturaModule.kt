package com.pingo.core.notifications.di

import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.AndroidNotificationHealthCheck
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.CapturaControllerImpl
import com.pingo.core.notifications.NotificationHealthCheck
import com.pingo.core.notifications.RebindDeListener
import com.pingo.core.notifications.RebindDeListenerDoSistema
import com.pingo.core.notifications.ServicoDeCaptura
import com.pingo.core.notifications.ServicoDeCapturaDoAndroid
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CapturaModule {
    @Binds
    @Singleton
    abstract fun bindCapturaController(implementation: CapturaControllerImpl): CapturaController

    @Binds
    @Singleton
    abstract fun bindAcessoNotificacoes(implementation: AcessoNotificacoesDoSistema): AcessoNotificacoes

    @Binds
    @Singleton
    abstract fun bindRebindDeListener(implementation: RebindDeListenerDoSistema): RebindDeListener

    @Binds
    @Singleton
    abstract fun bindServicoDeCaptura(implementation: ServicoDeCapturaDoAndroid): ServicoDeCaptura

    @Binds
    @Singleton
    abstract fun bindNotificationHealthCheck(implementation: AndroidNotificationHealthCheck): NotificationHealthCheck
}
