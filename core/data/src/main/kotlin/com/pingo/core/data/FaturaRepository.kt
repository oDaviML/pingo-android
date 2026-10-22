package com.pingo.core.data

import com.pingo.core.model.Fatura
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface FaturaRepository {
    fun observeFaturas(): Flow<List<Fatura>>

    fun observePorInstrumento(instrumentoPublicId: String): Flow<List<Fatura>>

    suspend fun buscar(publicId: String): Fatura?

    suspend fun salvar(fatura: Fatura)

    suspend fun salvarTodas(faturas: List<Fatura>)

    suspend fun excluir(publicId: String, quando: Instant)
}
