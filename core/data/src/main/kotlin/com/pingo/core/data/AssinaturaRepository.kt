package com.pingo.core.data

import com.pingo.core.model.Assinatura
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface AssinaturaRepository {
    fun observeAssinaturas(): Flow<List<Assinatura>>

    suspend fun buscar(publicId: String): Assinatura?

    suspend fun salvar(assinatura: Assinatura)

    suspend fun salvarTodas(assinaturas: List<Assinatura>)

    suspend fun excluir(publicId: String, quando: Instant)
}
