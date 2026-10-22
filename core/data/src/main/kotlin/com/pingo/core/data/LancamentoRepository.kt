package com.pingo.core.data

import com.pingo.core.model.Lancamento
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LancamentoRepository {
    fun observeLancamentos(): Flow<List<Lancamento>>

    fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>>

    suspend fun buscar(publicId: String): Lancamento?

    suspend fun buscarPorHash(hash: String): Lancamento?

    suspend fun salvar(lancamento: Lancamento)

    suspend fun salvarTodos(lancamentos: List<Lancamento>)

    suspend fun excluir(publicId: String, quando: Instant)
}
