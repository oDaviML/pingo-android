package com.pingo.core.data.impl

import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.RegraCategoriaDao
import com.pingo.core.model.RegraCategoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RegraCategoriaRepositoryImpl @Inject constructor(private val regraCategoriaDao: RegraCategoriaDao) : RegraCategoriaRepository {
    override fun observeRegras(): Flow<List<RegraCategoria>> =
        regraCategoriaDao.observeAtivas().map { regras -> regras.map { it.toModel() } }

    override suspend fun buscar(publicId: String): RegraCategoria? = regraCategoriaDao.buscar(publicId)?.toModel()

    override suspend fun buscarPorEstabelecimento(estabelecimento: String): RegraCategoria? =
        regraCategoriaDao.buscarPorEstabelecimento(estabelecimento)?.toModel()

    override suspend fun salvar(regra: RegraCategoria) {
        regraCategoriaDao.upsert(regra.toEntity())
    }

    override suspend fun salvarTodas(regras: List<RegraCategoria>) {
        regraCategoriaDao.upsertAll(regras.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        regraCategoriaDao.marcarExcluida(publicId, quando)
    }
}
