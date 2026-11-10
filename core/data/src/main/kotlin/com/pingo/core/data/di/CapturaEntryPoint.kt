package com.pingo.core.data.di

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CapturaEntryPoint {
    fun contaRepository(): ContaRepository

    fun instrumentoRepository(): InstrumentoRepository

    fun lancamentoRepository(): LancamentoRepository

    fun categoriaRepository(): CategoriaRepository
}
