package com.pingo.core.notifications.di

import com.pingo.core.data.RegraParserRepository
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.BateriaDoAparelho
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.GatilhoDeCapturaParada
import com.pingo.core.notifications.NotificacoesDoSistema
import com.pingo.core.notifications.SaudeDaCaptura
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CapturaStatusEntryPoint {
    fun capturaDataSource(): CapturaDataSource

    fun capturaController(): CapturaController

    fun gatilhoDeCapturaParada(): GatilhoDeCapturaParada

    fun saudeDaCaptura(): SaudeDaCaptura

    fun acessoNotificacoes(): AcessoNotificacoes

    fun notificacoesDoSistema(): NotificacoesDoSistema

    fun bateriaDoAparelho(): BateriaDoAparelho

    fun regraParserRepository(): RegraParserRepository
}
