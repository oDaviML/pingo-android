package com.pingo.core.data.impl

import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class LancamentoRepositoryImpl @Inject constructor(private val lancamentoDao: LancamentoDao) : LancamentoRepository {
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
        lancamentoDao.upsert(lancamento.toEntity())
    }

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        val ordenados = lancamentos.sortedBy { lancamento -> if (lancamento.compraPaiPublicId == null) 0 else 1 }
        val semEstorno = ordenados.map { lancamento -> lancamento.copy(estornoDePublicId = null).toEntity() }
        val comEstorno = ordenados
            .filter { lancamento -> lancamento.estornoDePublicId != null }
            .map { lancamento -> lancamento.toEntity() }

        lancamentoDao.upsertEmDuasPassadas(semEstorno, comEstorno)
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        lancamentoDao.marcarExcluido(publicId, quando)
    }
}
