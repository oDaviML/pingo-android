package com.pingo.core.data

import com.pingo.core.model.Categoria
import com.pingo.core.model.RegraCategoria
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.Flow

data class CategoriaComContagem(val categoria: Categoria, val quantidadeLancamentos: Int, val quantidadeRegras: Int)

data class RegraCategoriaComUso(val regra: RegraCategoria, val nomeCategoria: String, val quantidadeLancamentos: Int)

sealed interface ResultadoExclusaoCategoria {
    data object Excluida : ResultadoExclusaoCategoria

    data object NaoEncontrada : ResultadoExclusaoCategoria

    data object DestinoObrigatorio : ResultadoExclusaoCategoria

    data object DestinoInvalido : ResultadoExclusaoCategoria
}

interface GestaoDeCategoriasRepository {
    fun observeCategoriasComContagem(): Flow<List<CategoriaComContagem>>

    fun observeRegrasComUso(): Flow<List<RegraCategoriaComUso>>

    suspend fun criarCategoria(nome: String, tipo: TipoCategoria): ResultadoCategoria

    suspend fun renomearCategoria(publicId: String, novoNome: String): ResultadoCategoria

    suspend fun alternarArquivoCategoria(publicId: String, arquivada: Boolean)

    suspend fun excluirCategoria(publicId: String, categoriaDestinoPublicId: String? = null): ResultadoExclusaoCategoria

    suspend fun excluirRegra(publicId: String)
}
