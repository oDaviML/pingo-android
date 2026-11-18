package com.pingo.core.data.impl

import com.pingo.core.data.CategoriaComContagem
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.GestaoDeCategoriasRepository
import com.pingo.core.data.RegraCategoriaComUso
import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.ResultadoExclusaoCategoria
import com.pingo.core.database.dao.GestaoDeCategoriasDao
import com.pingo.core.database.dao.LancamentoCategoriaEstabelecimentoRow
import com.pingo.core.model.EstabelecimentoNormalizado
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestaoDeCategoriasRepositoryImpl @Inject constructor(
    private val categoriaRepository: CategoriaRepository,
    private val regraCategoriaRepository: RegraCategoriaRepository,
    private val gestaoDeCategoriasDao: GestaoDeCategoriasDao,
    private val clock: Clock,
) : GestaoDeCategoriasRepository {
    override fun observeCategoriasComContagem(): Flow<List<CategoriaComContagem>> = combine(
        categoriaRepository.observeCategorias(),
        regraCategoriaRepository.observeRegras(),
        gestaoDeCategoriasDao.observeLancamentosParaContagem(),
    ) { categorias, regras, lancamentos ->
        val contagemPorCategoria = lancamentos
            .mapNotNull { it.categoriaPublicId }
            .groupingBy { it }
            .eachCount()
        val regrasPorCategoria = regras.groupingBy { it.categoriaPublicId }.eachCount()

        categorias
            .sortedWith(compareBy({ it.arquivada }, { it.tipo.name }, { it.nome.lowercase() }))
            .map { categoria ->
                CategoriaComContagem(
                    categoria = categoria,
                    quantidadeLancamentos = contagemPorCategoria[categoria.publicId] ?: 0,
                    quantidadeRegras = regrasPorCategoria[categoria.publicId] ?: 0,
                )
            }
    }

    override fun observeRegrasComUso(): Flow<List<RegraCategoriaComUso>> = combine(
        regraCategoriaRepository.observeRegras(),
        categoriaRepository.observeCategorias(),
        gestaoDeCategoriasDao.observeLancamentosParaContagem(),
    ) { regras, categorias, lancamentos ->
        val categoriasPorId = categorias.associateBy { it.publicId }
        val contagemPorEstabelecimento = contagemPorEstabelecimento(lancamentos)

        regras
            .sortedBy { it.estabelecimentoNormalizado }
            .map { regra ->
                RegraCategoriaComUso(
                    regra = regra,
                    nomeCategoria = categoriasPorId[regra.categoriaPublicId]?.nome ?: "Categoria",
                    quantidadeLancamentos = contagemPorEstabelecimento[regra.estabelecimentoNormalizado] ?: 0,
                )
            }
    }

    override suspend fun criarCategoria(nome: String, tipo: TipoCategoria): ResultadoCategoria =
        categoriaRepository.criar(nome, tipo, clock.instant())

    override suspend fun renomearCategoria(publicId: String, novoNome: String): ResultadoCategoria =
        categoriaRepository.renomear(publicId, novoNome, clock.instant())

    override suspend fun alternarArquivoCategoria(publicId: String, arquivada: Boolean) {
        val categoria = categoriaRepository.buscar(publicId)?.takeIf { it.excluidoEm == null } ?: return
        categoriaRepository.salvar(categoria.copy(arquivada = arquivada, atualizadoEm = clock.instant()))
    }

    override suspend fun excluirCategoria(publicId: String, categoriaDestinoPublicId: String?): ResultadoExclusaoCategoria {
        val origem = categoriaRepository.buscar(publicId)?.takeIf { it.excluidoEm == null }
            ?: return ResultadoExclusaoCategoria.NaoEncontrada
        val lancamentosAtivos = gestaoDeCategoriasDao.contarLancamentosAtivosDaCategoria(publicId)
        val erroDestino = validarDestinoExclusao(origem.tipo, publicId, lancamentosAtivos, categoriaDestinoPublicId)
        if (erroDestino != null) {
            return erroDestino
        }

        gestaoDeCategoriasDao.excluirCategoriaAtomicamente(publicId, categoriaDestinoPublicId, clock.instant())
        return ResultadoExclusaoCategoria.Excluida
    }

    override suspend fun excluirRegra(publicId: String) {
        regraCategoriaRepository.excluir(publicId, clock.instant())
    }

    private suspend fun validarDestinoExclusao(
        tipoOrigem: TipoCategoria,
        origemId: String,
        lancamentosAtivos: Int,
        destinoId: String?,
    ): ResultadoExclusaoCategoria? {
        if (destinoId == null) {
            return if (lancamentosAtivos > 0) ResultadoExclusaoCategoria.DestinoObrigatorio else null
        }
        val destino = if (destinoId == origemId) null else categoriaRepository.buscar(destinoId)
        val destinoValido = destino != null && destino.podeSerEscolhida && destino.tipo == tipoOrigem
        return if (destinoValido) null else ResultadoExclusaoCategoria.DestinoInvalido
    }

    private fun contagemPorEstabelecimento(lancamentos: List<LancamentoCategoriaEstabelecimentoRow>): Map<String, Int> = lancamentos
        .mapNotNull { row -> EstabelecimentoNormalizado.de(row.estabelecimento) }
        .groupingBy { it }
        .eachCount()
}
