package com.pingo.core.data

import com.pingo.core.model.Categoria
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CategoriaRepository {
    fun observeCategorias(): Flow<List<Categoria>>

    fun observeEscolhiveis(): Flow<List<Categoria>>

    suspend fun buscar(publicId: String): Categoria?

    suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria

    suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria

    suspend fun arquivar(publicId: String, quando: Instant)

    suspend fun salvar(categoria: Categoria)

    suspend fun salvarTodas(categorias: List<Categoria>)

    suspend fun excluir(publicId: String, quando: Instant)
}
