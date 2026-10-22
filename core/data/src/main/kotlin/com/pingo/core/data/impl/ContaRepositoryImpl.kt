package com.pingo.core.data.impl

import com.pingo.core.data.ContaRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.ContaDao
import com.pingo.core.model.Conta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class ContaRepositoryImpl @Inject constructor(private val contaDao: ContaDao) : ContaRepository {
    override fun observeContas(): Flow<List<Conta>> = contaDao.observeAtivas().map { contas -> contas.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Conta? = contaDao.buscar(publicId)?.toModel()

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
