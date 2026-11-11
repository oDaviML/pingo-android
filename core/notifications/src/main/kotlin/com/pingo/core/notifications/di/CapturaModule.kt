package com.pingo.core.notifications.di

import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.AndroidNotificationHealthCheck
import com.pingo.core.notifications.BateriaDoAparelho
import com.pingo.core.notifications.BateriaDoAparelhoDoAndroid
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.CapturaControllerImpl
import com.pingo.core.notifications.FabricanteDoAparelho
import com.pingo.core.notifications.FabricanteDoAparelhoDoAndroid
import com.pingo.core.notifications.IniciadorDeObservacaoDePendencias
import com.pingo.core.notifications.MonitorDaCaptura
import com.pingo.core.notifications.NotificacoesDoSistema
import com.pingo.core.notifications.NotificacoesDoSistemaDoAndroid
import com.pingo.core.notifications.NotificadorDeCapturaParada
import com.pingo.core.notifications.NotificadorDeCapturaParadaDoSistema
import com.pingo.core.notifications.NotificationHealthCheck
import com.pingo.core.notifications.ObservadorDePendencias
import com.pingo.core.notifications.RebindDeListener
import com.pingo.core.notifications.RebindDeListenerDoSistema
import com.pingo.core.notifications.RevisaoNotificacoes
import com.pingo.core.notifications.RevisaoNotificacoesDoAndroid
import com.pingo.core.notifications.RevisaoPorNotificacao
import com.pingo.core.notifications.RevisaoPorNotificacaoConfigurada
import com.pingo.core.notifications.SaudeDaCaptura
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

    @Binds
    @Singleton
    abstract fun bindNotificacoesDoSistema(implementation: NotificacoesDoSistemaDoAndroid): NotificacoesDoSistema

    @Binds
    @Singleton
    abstract fun bindNotificadorDeCapturaParada(implementation: NotificadorDeCapturaParadaDoSistema): NotificadorDeCapturaParada

    @Binds
    @Singleton
    abstract fun bindFabricanteDoAparelho(implementation: FabricanteDoAparelhoDoAndroid): FabricanteDoAparelho

    @Binds
    @Singleton
    abstract fun bindBateriaDoAparelho(implementation: BateriaDoAparelhoDoAndroid): BateriaDoAparelho

    @Binds
    @Singleton
    abstract fun bindSaudeDaCaptura(implementation: MonitorDaCaptura): SaudeDaCaptura

    @Binds
    @Singleton
    abstract fun bindRevisaoPorNotificacao(implementation: RevisaoPorNotificacaoConfigurada): RevisaoPorNotificacao

    @Binds
    @Singleton
    abstract fun bindRevisaoNotificacoes(implementation: RevisaoNotificacoesDoAndroid): RevisaoNotificacoes

    @Binds
    @Singleton
    abstract fun bindIniciadorDeObservacao(implementation: ObservadorDePendencias): IniciadorDeObservacaoDePendencias
}
