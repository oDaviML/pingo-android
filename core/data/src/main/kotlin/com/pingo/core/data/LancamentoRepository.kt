package com.pingo.core.data

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LancamentoRepository {
    fun observeLancamentos(): Flow<List<Lancamento>>

    fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>>

    suspend fun buscar(publicId: String): Lancamento?

    suspend fun buscarPorHash(hash: String): Lancamento?

    suspend fun recentesDaInstituicao(
        instituicao: InstituicaoFinanceira,
        origem: OrigemLancamento,
        desde: Instant,
        ate: Instant,
    ): List<Lancamento>

    suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<Lancamento>

    suspend fun salvar(lancamento: Lancamento)

    suspend fun salvarTodos(lancamentos: List<Lancamento>)

    suspend fun excluir(publicId: String, quando: Instant)
}
