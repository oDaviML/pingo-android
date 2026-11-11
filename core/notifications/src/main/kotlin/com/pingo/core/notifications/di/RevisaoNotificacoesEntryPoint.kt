package com.pingo.core.notifications.di

import com.pingo.core.datastore.PermissoesDataSource
import com.pingo.core.notifications.ObservadorDePendencias
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RevisaoNotificacoesEntryPoint {
    fun permissoesDataSource(): PermissoesDataSource

    fun observadorDePendencias(): ObservadorDePendencias
}
