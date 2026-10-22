package com.pingo.core.data.impl

import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.model.Lancamento
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

    override suspend fun salvar(lancamento: Lancamento) {
        lancamentoDao.upsert(lancamento.toEntity())
    }

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        val ordenados = lancamentos.sortedBy { lancamento -> if (lancamento.compraPaiPublicId == null) 0 else 1 }

        lancamentoDao.upsertAll(ordenados.map { lancamento -> lancamento.copy(estornoDePublicId = null).toEntity() })

        val comEstorno = ordenados.filter { lancamento -> lancamento.estornoDePublicId != null }

        if (comEstorno.isNotEmpty()) {
            lancamentoDao.upsertAll(comEstorno.map { lancamento -> lancamento.toEntity() })
        }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        lancamentoDao.marcarExcluido(publicId, quando)
    }
}
