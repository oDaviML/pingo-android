package com.pingo.core.data.impl

import com.pingo.core.data.FaturaRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.FaturaDao
import com.pingo.core.model.Fatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class FaturaRepositoryImpl @Inject constructor(private val faturaDao: FaturaDao) : FaturaRepository {
    override fun observeFaturas(): Flow<List<Fatura>> = faturaDao.observeAtivas().map { faturas -> faturas.map { it.toModel() } }

    override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<Fatura>> =
        faturaDao.observePorInstrumento(instrumentoPublicId).map { faturas -> faturas.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Fatura? = faturaDao.buscar(publicId)?.toModel()

    override suspend fun salvar(fatura: Fatura) {
        faturaDao.upsert(fatura.toEntity())
    }

    override suspend fun salvarTodas(faturas: List<Fatura>) {
        faturaDao.upsertAll(faturas.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        faturaDao.marcarExcluida(publicId, quando)
    }
}
