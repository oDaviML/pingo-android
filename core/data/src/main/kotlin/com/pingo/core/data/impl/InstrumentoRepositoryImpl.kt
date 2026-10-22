package com.pingo.core.data.impl

import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.model.Instrumento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class InstrumentoRepositoryImpl @Inject constructor(private val instrumentoDao: InstrumentoDao) : InstrumentoRepository {
    override fun observeInstrumentos(): Flow<List<Instrumento>> =
        instrumentoDao.observeAtivos().map { instrumentos -> instrumentos.map { it.toModel() } }

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
        instrumentoDao.observePorConta(contaPublicId).map { instrumentos -> instrumentos.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Instrumento? = instrumentoDao.buscar(publicId)?.toModel()

    override suspend fun salvar(instrumento: Instrumento) {
        instrumentoDao.upsert(instrumento.toEntity())
    }

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) {
        instrumentoDao.upsertAll(instrumentos.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        instrumentoDao.marcarExcluido(publicId, quando)
    }
}
