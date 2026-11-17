package com.pingo.core.notifications

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

internal class FakeLancamentoRepository(iniciais: List<Lancamento> = emptyList()) : LancamentoRepository {
    private val todos = MutableStateFlow(iniciais)

    override fun observeLancamentos(): Flow<List<Lancamento>> = todos.map { lista ->
        lista.filter { it.excluidoEm == null }.sortedByDescending { it.dataHora }
    }

    override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>> = observeLancamentos().map { lista ->
        lista.filter { it.dataHora in inicio..fim }
    }

    override suspend fun buscar(publicId: String): Lancamento? = todos.value.firstOrNull { it.publicId == publicId }

    override suspend fun buscarPorHash(hash: String): Lancamento? = todos.value.firstOrNull { it.hashDuplicata == hash }

    override suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String? = todos.value
        .filter { it.excluidoEm == null && it.instrumentoPublicId in instrumentos }
        .maxByOrNull { it.dataHora }
        ?.instrumentoPublicId

    override suspend fun capturadosSemelhantes(
        instrumentoPublicId: String,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
    ): List<Lancamento> = emptyList()

    override suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<Lancamento> = emptyList()

    override suspend fun salvar(lancamento: Lancamento) {
        todos.update { lista -> lista.filterNot { it.publicId == lancamento.publicId } + lancamento }
    }

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        lancamentos.forEach { salvar(it) }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        val atual = buscar(publicId) ?: return
        salvar(atual.copy(excluidoEm = quando, atualizadoEm = quando))
    }

    fun atual(publicId: String): Lancamento? = todos.value.firstOrNull { it.publicId == publicId }

    fun todos(): List<Lancamento> = todos.value
}

internal class FakeCategoriaRepository(iniciais: List<Categoria> = emptyList()) : CategoriaRepository {
    private val todas = MutableStateFlow(iniciais)

    override fun observeCategorias(): Flow<List<Categoria>> = todas

    override fun observeEscolhiveis(): Flow<List<Categoria>> = todas.map { lista -> lista.filter { it.podeSerEscolhida } }

    override suspend fun buscar(publicId: String): Categoria? = todas.value.firstOrNull { it.publicId == publicId }

    override suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria = ResultadoCategoria.NaoEncontrada

    override suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria =
        ResultadoCategoria.NaoEncontrada

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun salvar(categoria: Categoria) {
        todas.update { lista -> lista.filterNot { it.publicId == categoria.publicId } + categoria }
    }

    override suspend fun salvarTodas(categorias: List<Categoria>) {
        categorias.forEach { salvar(it) }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        val atual = buscar(publicId) ?: return
        salvar(atual.copy(excluidoEm = quando, atualizadoEm = quando))
    }
}

internal class FakeInstrumentoRepository(iniciais: List<Instrumento> = emptyList()) : InstrumentoRepository {
    private val todos = MutableStateFlow(iniciais)

    override fun observeInstrumentos(): Flow<List<Instrumento>> = todos

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> = todos.map { lista ->
        lista.filter { it.contaPublicId == contaPublicId }
    }

    override suspend fun buscar(publicId: String): Instrumento? = todos.value.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> = todos.value

    override suspend fun salvar(instrumento: Instrumento) {
        todos.update { lista -> lista.filterNot { it.publicId == instrumento.publicId } + instrumento }
    }

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) {
        instrumentos.forEach { salvar(it) }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        val atual = buscar(publicId) ?: return
        salvar(atual.copy(excluidoEm = quando, atualizadoEm = quando))
    }

    override suspend fun arquivar(publicId: String, quando: Instant) {
        val atual = buscar(publicId) ?: return
        salvar(atual.copy(arquivado = true, atualizadoEm = quando))
    }

    override suspend fun reativar(publicId: String, quando: Instant) {
        val atual = buscar(publicId) ?: return
        salvar(atual.copy(arquivado = false, arquivadoPorCascata = false, atualizadoEm = quando))
    }
}

internal data class PostagemDeTeste(val pendencia: PendenciaParaNotificar, val precisaCorrigir: Boolean)

internal class FakeRevisaoNotificacoes(var habilitada: Boolean = true) : RevisaoNotificacoes {
    val postagens = mutableListOf<PostagemDeTeste>()
    val canceladas = mutableListOf<String>()
    val ausentes = mutableListOf<Set<String>>()
    var aceitaPostagem: Boolean = true

    override suspend fun habilitada(): Boolean = habilitada

    override fun postar(pendencia: PendenciaParaNotificar, precisaCorrigir: Boolean): Boolean {
        postagens += PostagemDeTeste(pendencia, precisaCorrigir)
        return aceitaPostagem
    }

    override fun cancelar(publicId: String) {
        canceladas += publicId
    }

    override fun cancelarAusentes(publicIds: Set<String>) {
        ausentes += publicIds
    }
}
