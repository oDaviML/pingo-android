package com.pingo.core.data

import com.pingo.core.model.Categoria
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CategoriaRepository {
    fun observeCategorias(): Flow<List<Categoria>>

    suspend fun buscar(publicId: String): Categoria?

    suspend fun salvar(categoria: Categoria)

    suspend fun salvarTodas(categorias: List<Categoria>)

    suspend fun excluir(publicId: String, quando: Instant)
}
