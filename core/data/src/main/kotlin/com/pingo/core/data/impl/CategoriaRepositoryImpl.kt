package com.pingo.core.data.impl

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.model.Categoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class CategoriaRepositoryImpl @Inject constructor(private val categoriaDao: CategoriaDao) : CategoriaRepository {
    override fun observeCategorias(): Flow<List<Categoria>> =
        categoriaDao.observeAtivas().map { categorias -> categorias.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Categoria? = categoriaDao.buscar(publicId)?.toModel()

    override suspend fun salvar(categoria: Categoria) {
        categoriaDao.upsert(categoria.toEntity())
    }

    override suspend fun salvarTodas(categorias: List<Categoria>) {
        categoriaDao.upsertAll(categorias.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        categoriaDao.marcarExcluida(publicId, quando)
    }
}
