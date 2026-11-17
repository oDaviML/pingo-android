package com.pingo.feature.review.impl

import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.RegraCategoria
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.Base64

class FakeLancamentoRepository(iniciais: List<Lancamento> = emptyList()) : LancamentoRepository {
    private val todos = MutableStateFlow(iniciais)

    override fun observeLancamentos(): Flow<List<Lancamento>> = todos.map { lancamentos ->
        lancamentos.filter { it.excluidoEm == null }.sortedByDescending { it.dataHora }
    }

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
        todos.update { lancamentos -> lancamentos.filterNot { it.publicId == lancamento.publicId } + lancamento }
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

class FakeContaRepository(iniciais: List<Conta> = emptyList()) : ContaRepository {
    private val contas = MutableStateFlow(iniciais)

    override fun observeContas(): Flow<List<Conta>> = contas

    override suspend fun buscar(publicId: String): Conta? = contas.value.firstOrNull { it.publicId == publicId }

    override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant, publicIds: List<String>) = emptyList<CadastroSalvo>()

    override suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo = error("nao usado")

    override suspend fun salvar(conta: Conta) {
        contas.update { atuais -> atuais.filterNot { it.publicId == conta.publicId } + conta }
    }

    override suspend fun salvarTodas(contas: List<Conta>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun reativar(publicId: String, quando: Instant) = Unit
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

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun reativar(publicId: String, quando: Instant) = Unit

    fun todos(): List<Instrumento> = instrumentos.value
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

class FakeRegraCategoriaRepository(iniciais: List<RegraCategoria> = emptyList()) : RegraCategoriaRepository {
    private val regras = MutableStateFlow(iniciais)

    override fun observeRegras(): Flow<List<RegraCategoria>> = regras

    override suspend fun buscar(publicId: String): RegraCategoria? = regras.value.firstOrNull { it.publicId == publicId }

    override suspend fun buscarPorEstabelecimento(estabelecimento: String): RegraCategoria? = regras.value.firstOrNull { regra ->
        regra.estabelecimentoNormalizado == estabelecimento && regra.excluidoEm == null
    }

    override suspend fun salvar(regra: RegraCategoria) {
        regras.update { atuais -> atuais.filterNot { it.publicId == regra.publicId } + regra }
    }

    override suspend fun salvarTodas(regras: List<RegraCategoria>) {
        regras.forEach { salvar(it) }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        regras.update { atuais -> atuais.map { if (it.publicId == publicId) it.copy(excluidoEm = quando) else it } }
    }

    fun todas(): List<RegraCategoria> = regras.value
}

class FakeTransacaoLocal : TransacaoLocal {
    var execucoes: Int = 0

    override suspend fun <T> executar(bloco: suspend () -> T): T {
        execucoes++

        return bloco()
    }
}

class FakeTokenStore(private var tokens: StoredTokens?) : TokenStore {
    override suspend fun read(): StoredTokens? = tokens

    override suspend fun write(tokens: StoredTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

internal fun tokensDoUsuario(sub: String): StoredTokens = StoredTokens(
    accessToken = jwtComPayload("""{"sub":"$sub"}"""),
    refreshToken = null,
    expiresAtEpochMillis = Long.MAX_VALUE,
)

private fun jwtComPayload(payload: String): String {
    val codificador = Base64.getUrlEncoder().withoutPadding()
    val cabecalho = codificador.encodeToString("""{"alg":"RS256"}""".toByteArray())

    return listOf(cabecalho, codificador.encodeToString(payload.toByteArray()), "assinatura").joinToString(".")
}

@Suppress("LongParameterList")
internal fun pendenciaDeTeste(
    publicId: String,
    dataHora: Instant,
    valorCentavos: Long = 4290,
    tipo: TipoLancamento = TipoLancamento.DESPESA,
    status: StatusLancamento = StatusLancamento.PENDENTE,
    origem: OrigemLancamento = OrigemLancamento.NOTIFICACAO,
    estabelecimento: String? = "Padaria do Zé",
    instrumentoPublicId: String? = null,
    categoriaPublicId: String? = null,
    instituicaoOrigem: InstituicaoFinanceira? = null,
    textoBruto: String? = null,
    transferenciaId: String? = null,
    motivoPendencia: String? = null,
    confianca: Double = 0.62,
): Lancamento = Lancamento(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    transferenciaId = transferenciaId,
    valorCentavos = valorCentavos,
    dataHora = dataHora,
    tipo = tipo,
    estabelecimento = estabelecimento,
    status = status,
    origem = origem,
    instituicaoOrigem = instituicaoOrigem,
    confianca = confianca,
    motivoPendencia = motivoPendencia,
    criadoEm = dataHora,
    atualizadoEm = dataHora,
    textoBruto = textoBruto,
)
