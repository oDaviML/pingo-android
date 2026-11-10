package com.pingo.core.data.impl

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.categoria.nomeLimpo
import com.pingo.core.data.categoria.ocupaNome
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.model.Categoria
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CategoriaRepositoryImpl @Inject constructor(private val categoriaDao: CategoriaDao) : CategoriaRepository {
    override fun observeCategorias(): Flow<List<Categoria>> =
        categoriaDao.observeAtivas().map { categorias -> categorias.map { it.toModel() } }

    override fun observeEscolhiveis(): Flow<List<Categoria>> =
        observeCategorias().map { categorias -> categorias.filter { it.podeSerEscolhida } }

    override suspend fun buscar(publicId: String): Categoria? = categoriaDao.buscar(publicId)?.toModel()

    override suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria {
        val nomeLimpo = nome.nomeLimpo()
        erroDeNome(nomeLimpo, tipo, ignorando = null)?.let { return it }

        val categoria = Categoria(
            publicId = UUID.randomUUID().toString(),
            nome = nomeLimpo,
            tipo = tipo,
            criadoEm = agora,
            atualizadoEm = agora,
        )
        categoriaDao.upsert(categoria.toEntity())

        return ResultadoCategoria.Sucesso(categoria)
    }

    override suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria {
        val atual = categoriaAtiva(publicId) ?: return ResultadoCategoria.NaoEncontrada
        val nomeLimpo = novoNome.nomeLimpo()
        erroDeNome(nomeLimpo, atual.tipo, ignorando = publicId)?.let { return it }

        val renomeada = atual.copy(nome = nomeLimpo, atualizadoEm = quando)
        categoriaDao.upsert(renomeada.toEntity())

        return ResultadoCategoria.Sucesso(renomeada)
    }

    override suspend fun arquivar(publicId: String, quando: Instant) {
        val categoria = categoriaAtiva(publicId) ?: return
        categoriaDao.upsert(categoria.copy(arquivada = true, atualizadoEm = quando).toEntity())
    }

    override suspend fun salvar(categoria: Categoria) {
        categoriaDao.upsert(categoria.toEntity())
    }

    override suspend fun salvarTodas(categorias: List<Categoria>) {
        categoriaDao.upsertAll(categorias.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        categoriaDao.marcarExcluida(publicId, quando)
    }

    private suspend fun categoriaAtiva(publicId: String): Categoria? =
        categoriaDao.buscar(publicId)?.toModel()?.takeIf { it.excluidoEm == null }

    private suspend fun erroDeNome(nome: String, tipo: TipoCategoria, ignorando: String?): ResultadoCategoria? = when {
        nome.isEmpty() -> ResultadoCategoria.NomeVazio
        observeCategorias().first().any { it.publicId != ignorando && it.ocupaNome(nome, tipo) } -> ResultadoCategoria.NomeEmUso
        else -> null
    }
}
