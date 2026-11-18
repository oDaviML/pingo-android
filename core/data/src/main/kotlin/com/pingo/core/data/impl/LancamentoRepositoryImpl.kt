package com.pingo.core.data.impl

import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.reconciliacao.ReconciliadorFinanceiro
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class LancamentoRepositoryImpl @Inject constructor(
    private val lancamentoDao: LancamentoDao,
    private val transacaoLocal: TransacaoLocal = TransacaoLocal.Direta,
    private val reconciliador: ReconciliadorFinanceiro = ReconciliadorFinanceiro.NoOp,
) : LancamentoRepository {
    override fun observeLancamentos(): Flow<List<Lancamento>> =
        lancamentoDao.observeAtivos().map { lancamentos -> lancamentos.map { it.toModel() } }

    override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>> =
        lancamentoDao.observePorPeriodo(inicio, fim).map { lancamentos -> lancamentos.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Lancamento? = lancamentoDao.buscar(publicId)?.toModel()

    override suspend fun buscarPorHash(hash: String): Lancamento? = lancamentoDao.buscarPorHash(hash)?.toModel()

    override suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String? = lancamentoDao.ultimoInstrumentoUsado(instrumentos)

    override suspend fun capturadosSemelhantes(
        instrumentoPublicId: String,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
    ): List<Lancamento> = lancamentoDao
        .capturadosSemelhantes(instrumentoPublicId, valorCentavos, inicio, fim, OrigemLancamento.NOTIFICACAO)
        .map { it.toModel() }

    override suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<Lancamento> = lancamentoDao
        .candidatosDeTransferencia(tipo, valorCentavos, inicio, fim, contaPublicId, origem)
        .map { it.toModel() }

    override suspend fun salvar(lancamento: Lancamento) {
        transacaoLocal.executar {
            val antes = lancamentoDao.buscar(lancamento.publicId)?.toModel()
            val normalizado = enriquecerNormalizacao(lancamento)
            lancamentoDao.upsert(normalizado.toEntity())
            reconciliador.aoAlterarLancamentos(listOf(antes to normalizado))
        }
    }

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        if (lancamentos.isEmpty()) {
            return
        }
        transacaoLocal.executar {
            val paresAntes = lancamentos.map { item -> lancamentoDao.buscar(item.publicId)?.toModel() }
            val normalizados = lancamentos.map { enriquecerNormalizacao(it) }
            val ordenados = normalizados.sortedBy { lancamento -> if (lancamento.compraPaiPublicId == null) 0 else 1 }
            val semEstorno = ordenados.map { lancamento -> lancamento.copy(estornoDePublicId = null).toEntity() }
            val comEstorno = ordenados
                .filter { lancamento -> lancamento.estornoDePublicId != null }
                .map { lancamento -> lancamento.toEntity() }
            lancamentoDao.upsertEmDuasPassadas(semEstorno, comEstorno)
            reconciliador.aoAlterarLancamentos(paresAntes.zip(normalizados))
        }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        transacaoLocal.executar {
            val antes = lancamentoDao.buscar(publicId)?.toModel()
            lancamentoDao.marcarExcluido(publicId, quando)
            val depois = lancamentoDao.buscar(publicId)?.toModel()
            if (antes != null || depois != null) {
                reconciliador.aoAlterarLancamentos(listOf(antes to depois))
            }
        }
    }

    private fun enriquecerNormalizacao(lancamento: Lancamento): Lancamento {
        val normalizado = lancamento.estabelecimentoNormalizado
            ?: NormalizadorDeAssinatura.normalizar(lancamento.estabelecimento)?.take(LIMITE_ESTABELECIMENTO_NORMALIZADO)
        return if (normalizado == lancamento.estabelecimentoNormalizado) {
            lancamento
        } else {
            lancamento.copy(estabelecimentoNormalizado = normalizado)
        }
    }

    private companion object {
        const val LIMITE_ESTABELECIMENTO_NORMALIZADO = 200
    }
}
