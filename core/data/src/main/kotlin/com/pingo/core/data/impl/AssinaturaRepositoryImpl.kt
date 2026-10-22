package com.pingo.core.data.impl

import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.AssinaturaDao
import com.pingo.core.model.Assinatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class AssinaturaRepositoryImpl @Inject constructor(private val assinaturaDao: AssinaturaDao) : AssinaturaRepository {
    override fun observeAssinaturas(): Flow<List<Assinatura>> =
        assinaturaDao.observeAtivas().map { assinaturas -> assinaturas.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Assinatura? = assinaturaDao.buscar(publicId)?.toModel()

    override suspend fun salvar(assinatura: Assinatura) {
        assinaturaDao.upsert(assinatura.toEntity())
    }

    override suspend fun salvarTodas(assinaturas: List<Assinatura>) {
        assinaturaDao.upsertAll(assinaturas.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        assinaturaDao.marcarExcluida(publicId, quando)
    }
}
