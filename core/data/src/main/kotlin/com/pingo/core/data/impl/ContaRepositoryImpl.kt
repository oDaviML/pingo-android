package com.pingo.core.data.impl

import com.pingo.core.data.ContaRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.ContaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.model.Conta
import com.pingo.core.model.NovaConta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class ContaRepositoryImpl @Inject constructor(
    private val contaDao: ContaDao,
    private val instrumentoDao: InstrumentoDao,
    private val transacao: TransacaoLocal,
) : ContaRepository {
    override fun observeContas(): Flow<List<Conta>> = contaDao.observeAtivas().map { contas -> contas.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Conta? = contaDao.buscar(publicId)?.toModel()

    override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant) {
        require(contas.all(NovaConta::valida)) { "Conta ou instrumento inválido" }

        val criadas = contas.map { it.criar(agora) }

        transacao.executar {
            contaDao.upsertAll(criadas.map { it.conta.toEntity() })
            instrumentoDao.upsertAll(criadas.flatMap { it.instrumentos }.map { it.toEntity() })
        }
    }

    override suspend fun salvar(conta: Conta) {
        contaDao.upsert(conta.toEntity())
    }

    override suspend fun salvarTodas(contas: List<Conta>) {
        contaDao.upsertAll(contas.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        contaDao.marcarExcluida(publicId, quando)
    }
}
