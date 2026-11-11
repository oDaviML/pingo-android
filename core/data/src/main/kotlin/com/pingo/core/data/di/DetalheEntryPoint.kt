package com.pingo.core.data.di

import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DetalheEntryPoint {
    fun editarLancamento(): EditarLancamento

    fun marcarTransferencia(): MarcarTransferencia
}
