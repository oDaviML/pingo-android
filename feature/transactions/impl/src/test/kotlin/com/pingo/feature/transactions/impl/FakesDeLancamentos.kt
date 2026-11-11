package com.pingo.feature.transactions.impl

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeLancamentoRepository(iniciais: List<Lancamento> = emptyList()) : LancamentoRepository {
    private val todos = MutableStateFlow(iniciais)

    override fun observeLancamentos(): Flow<List<Lancamento>> = todos.map { lancamentos -> lancamentos.filter { it.excluidoEm == null } }

    override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>> =
        observeLancamentos().map { lancamentos -> lancamentos.filter { it.dataHora in inicio..fim } }

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
    ): List<Lancamento> = todos.value.filter { lancamento ->
        lancamento.excluidoEm == null &&
            lancamento.origem == OrigemLancamento.NOTIFICACAO &&
            lancamento.instrumentoPublicId == instrumentoPublicId &&
            lancamento.valorCentavos == valorCentavos &&
            lancamento.dataHora in inicio..fim
    }

    override suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<Lancamento> = emptyList()

    override suspend fun salvar(lancamento: Lancamento) {
        todos.update { lancamentos -> lancamentos.filterNot { it.publicId == lancamento.publicId } + lancamento }
    }

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        lancamentos.forEach { salvar(it) }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        val atual = buscar(publicId) ?: return
        salvar(atual.copy(excluidoEm = quando))
    }

    fun atual(publicId: String): Lancamento? = todos.value.firstOrNull { it.publicId == publicId }

    fun publicar(lancamentos: List<Lancamento>) {
        todos.value = lancamentos
    }
}

class FakeContaRepository(iniciais: List<Conta> = emptyList()) : ContaRepository {
    private val contas = MutableStateFlow(iniciais)

    override fun observeContas(): Flow<List<Conta>> = contas

    override suspend fun buscar(publicId: String): Conta? = contas.value.firstOrNull { it.publicId == publicId }

    override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant) = Unit

    override suspend fun salvar(conta: Conta) {
        contas.update { atuais -> atuais.filterNot { it.publicId == conta.publicId } + conta }
    }

    override suspend fun salvarTodas(contas: List<Conta>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit
}

class FakeInstrumentoRepository(iniciais: List<Instrumento> = emptyList()) : InstrumentoRepository {
    private val instrumentos = MutableStateFlow(iniciais)

    override fun observeInstrumentos(): Flow<List<Instrumento>> = instrumentos

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
        instrumentos.map { lista -> lista.filter { it.contaPublicId == contaPublicId } }

    override suspend fun buscar(publicId: String): Instrumento? = instrumentos.value.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> =
        instrumentos.value.filter { it.arquivado.not() }

    override suspend fun salvar(instrumento: Instrumento) = Unit

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit
}

class FakeCategoriaRepository(iniciais: List<Categoria> = emptyList()) : CategoriaRepository {
    private val categorias = MutableStateFlow(iniciais)

    override fun observeCategorias(): Flow<List<Categoria>> = categorias

    override fun observeEscolhiveis(): Flow<List<Categoria>> = categorias.map { lista -> lista.filter { it.podeSerEscolhida } }

    override suspend fun buscar(publicId: String): Categoria? = categorias.value.firstOrNull { it.publicId == publicId }

    override suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria = ResultadoCategoria.NomeVazio

    override suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria =
        ResultadoCategoria.NaoEncontrada

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun salvar(categoria: Categoria) = Unit

    override suspend fun salvarTodas(categorias: List<Categoria>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit
}

@Suppress("LongParameterList")
internal fun lancamentoDeTeste(
    publicId: String,
    dataHora: Instant,
    valorCentavos: Long,
    tipo: TipoLancamento = TipoLancamento.DESPESA,
    status: StatusLancamento = StatusLancamento.CONFIRMADO,
    origem: OrigemLancamento = OrigemLancamento.NOTIFICACAO,
    estabelecimento: String? = "Estabelecimento",
    instrumentoPublicId: String? = null,
    categoriaPublicId: String? = null,
    instituicaoOrigem: InstituicaoFinanceira? = null,
): Lancamento = Lancamento(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    valorCentavos = valorCentavos,
    dataHora = dataHora,
    tipo = tipo,
    estabelecimento = estabelecimento,
    status = status,
    origem = origem,
    instituicaoOrigem = instituicaoOrigem,
    confianca = 1.0,
    criadoEm = dataHora,
    atualizadoEm = dataHora,
)
