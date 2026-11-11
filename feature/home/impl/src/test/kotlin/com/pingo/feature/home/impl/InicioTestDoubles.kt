package com.pingo.feature.home.impl

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
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.DiagnosticoDaCaptura
import com.pingo.core.notifications.SaudeDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

@Suppress("LongParameterList")
internal fun lancamentoDeTeste(
    publicId: String,
    valorCentavos: Long,
    dataHora: Instant,
    tipo: TipoLancamento = TipoLancamento.DESPESA,
    status: StatusLancamento = StatusLancamento.CONFIRMADO,
    instrumentoPublicId: String? = INSTRUMENTO_PADRAO,
    categoriaPublicId: String? = null,
    estabelecimento: String? = "Padaria do Zé",
    excluidoEm: Instant? = null,
): Lancamento = Lancamento(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    valorCentavos = valorCentavos,
    dataHora = dataHora,
    tipo = tipo,
    estabelecimento = estabelecimento,
    status = status,
    origem = OrigemLancamento.NOTIFICACAO,
    instituicaoOrigem = InstituicaoFinanceira.ITAU,
    confianca = 0.9,
    criadoEm = dataHora,
    atualizadoEm = dataHora,
    excluidoEm = excluidoEm,
)

internal fun categoriaDeTeste(publicId: String, nome: String, tipo: TipoCategoria = TipoCategoria.DESPESA): Categoria = Categoria(
    publicId = publicId,
    nome = nome,
    tipo = tipo,
    criadoEm = Instant.parse("2026-11-01T00:00:00Z"),
    atualizadoEm = Instant.parse("2026-11-01T00:00:00Z"),
)

internal fun instrumentoDeTeste(publicId: String = INSTRUMENTO_PADRAO, contaPublicId: String = CONTA_PADRAO): Instrumento = Instrumento(
    publicId = publicId,
    contaPublicId = contaPublicId,
    tipo = com.pingo.core.model.TipoInstrumento.CREDITO,
    apelido = "Cartão Itaú",
    criadoEm = Instant.parse("2026-11-01T00:00:00Z"),
    atualizadoEm = Instant.parse("2026-11-01T00:00:00Z"),
)

internal fun contaDeTeste(publicId: String = CONTA_PADRAO): Conta = Conta(
    publicId = publicId,
    instituicao = InstituicaoFinanceira.ITAU,
    apelido = "Itaú",
    tipo = com.pingo.core.model.TipoConta.CORRENTE,
    criadoEm = Instant.parse("2026-11-01T00:00:00Z"),
    atualizadoEm = Instant.parse("2026-11-01T00:00:00Z"),
)

internal class FakeLancamentoRepository(lancamentos: List<Lancamento> = emptyList()) : LancamentoRepository {
    val fluxo = MutableStateFlow(lancamentos)

    override fun observeLancamentos(): Flow<List<Lancamento>> = fluxo

    override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>> = error("nao usado")

    override suspend fun buscar(publicId: String): Lancamento? = fluxo.value.firstOrNull { it.publicId == publicId }

    override suspend fun buscarPorHash(hash: String): Lancamento? = error("nao usado")

    override suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String? = error("nao usado")

    override suspend fun capturadosSemelhantes(
        instrumentoPublicId: String,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
    ): List<Lancamento> = error("nao usado")

    override suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<Lancamento> = error("nao usado")

    override suspend fun salvar(lancamento: Lancamento) {
        fluxo.value = fluxo.value.filterNot { it.publicId == lancamento.publicId } + lancamento
    }

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        lancamentos.forEach { salvar(it) }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        fluxo.value = fluxo.value.map { if (it.publicId == publicId) it.copy(excluidoEm = quando) else it }
    }
}

internal class FakeCategoriaRepository(categorias: List<Categoria> = emptyList()) : CategoriaRepository {
    val fluxo = MutableStateFlow(categorias)

    override fun observeCategorias(): Flow<List<Categoria>> = fluxo

    override fun observeEscolhiveis(): Flow<List<Categoria>> = fluxo

    override suspend fun buscar(publicId: String): Categoria? = fluxo.value.firstOrNull { it.publicId == publicId }

    override suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria = error("nao usado")

    override suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria = error("nao usado")

    override suspend fun arquivar(publicId: String, quando: Instant) = error("nao usado")

    override suspend fun salvar(categoria: Categoria) = error("nao usado")

    override suspend fun salvarTodas(categorias: List<Categoria>) = error("nao usado")

    override suspend fun excluir(publicId: String, quando: Instant) = error("nao usado")
}

internal class FakeContaRepository(contas: List<Conta> = emptyList()) : ContaRepository {
    val fluxo = MutableStateFlow(contas)

    override fun observeContas(): Flow<List<Conta>> = fluxo

    override suspend fun buscar(publicId: String): Conta? = fluxo.value.firstOrNull { it.publicId == publicId }

    override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant) = error("nao usado")

    override suspend fun salvar(conta: Conta) = error("nao usado")

    override suspend fun salvarTodas(contas: List<Conta>) = error("nao usado")

    override suspend fun excluir(publicId: String, quando: Instant) = error("nao usado")
}

internal class FakeInstrumentoRepository(instrumentos: List<Instrumento> = emptyList()) : InstrumentoRepository {
    val fluxo = MutableStateFlow(instrumentos)

    override fun observeInstrumentos(): Flow<List<Instrumento>> = fluxo

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> = error("nao usado")

    override suspend fun buscar(publicId: String): Instrumento? = fluxo.value.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> = error("nao usado")

    override suspend fun salvar(instrumento: Instrumento) = error("nao usado")

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) = error("nao usado")

    override suspend fun excluir(publicId: String, quando: Instant) = error("nao usado")
}

internal class FakeAcessoNotificacoes(var concedido: Boolean) : AcessoNotificacoes {
    override fun concedido(): Boolean = concedido
}

internal class FakeSaudeDaCaptura(diagnostico: DiagnosticoDaCaptura = DiagnosticoDaCaptura(situacao = SituacaoDaCaptura.ATIVA)) :
    SaudeDaCaptura {
    val diagnostico = MutableStateFlow(diagnostico)

    override fun observar(): Flow<DiagnosticoDaCaptura> = diagnostico

    override suspend fun atual(): DiagnosticoDaCaptura = diagnostico.value
}

internal const val INSTRUMENTO_PADRAO = "instrumento-1"
internal const val CONTA_PADRAO = "conta-1"
