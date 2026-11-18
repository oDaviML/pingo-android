package com.pingo.core.data

import com.pingo.core.model.fatura.VigenciaDeCartao
import kotlinx.coroutines.flow.Flow

interface ConfiguracaoCartaoRepository {
    fun observePorInstrumento(instrumentoPublicId: String): Flow<List<VigenciaDeCartao>>

    suspend fun listarPorInstrumento(instrumentoPublicId: String): List<VigenciaDeCartao>

    suspend fun listarAtivas(): List<VigenciaDeCartao>
}
