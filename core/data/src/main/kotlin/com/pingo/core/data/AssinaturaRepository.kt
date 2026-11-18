package com.pingo.core.data

import com.pingo.core.model.Assinatura
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.assinatura.SugestaoDeAssinatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant

interface AssinaturaRepository {
    fun observeAssinaturas(): Flow<List<Assinatura>>

    fun observeConfirmadas(): Flow<List<Assinatura>> = observeAssinaturas().map { lista ->
        lista.filter { it.status == StatusAssinatura.CONFIRMADA && it.excluidoEm == null }
    }

    fun observeSugestoes(): Flow<List<SugestaoDeAssinatura>> = flowOf(emptyList())

    fun observeCobrancasDaAssinatura(assinaturaPublicId: String): Flow<List<Lancamento>> = flowOf(emptyList())

    suspend fun buscar(publicId: String): Assinatura?

    suspend fun salvar(assinatura: Assinatura)

    suspend fun salvarTodas(assinaturas: List<Assinatura>)

    suspend fun excluir(publicId: String, quando: Instant)

    suspend fun confirmarSugestao(sugestao: SugestaoDeAssinatura): Assinatura = throw UnsupportedOperationException()

    suspend fun recusarSugestao(sugestao: SugestaoDeAssinatura): Assinatura = throw UnsupportedOperationException()

    suspend fun cancelarAssinatura(assinaturaPublicId: String) = Unit
}
