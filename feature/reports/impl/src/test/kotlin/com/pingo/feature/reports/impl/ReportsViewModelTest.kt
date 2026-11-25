package com.pingo.feature.reports.impl

import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.projecao.RelatoriosProjection
import com.pingo.core.model.Assinatura
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.ChaveAssinatura
import com.pingo.core.model.assinatura.SugestaoDeAssinatura
import com.pingo.core.model.relatorio.AgregadorDeRelatorioMensal
import com.pingo.core.model.relatorio.PontoSerieMensal
import com.pingo.core.model.relatorio.RelatorioMensal
import com.pingo.core.model.relatorio.RelatorioMensalDetalhado
import com.pingo.core.model.relatorio.ResumoCategoriaRelatorio
import com.pingo.core.model.relatorio.VariacaoCategoria
import com.pingo.core.testing.FakeAppDispatchers
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val agora = Instant.parse("2026-11-10T15:00:00Z")
    private val clock = Clock.fixed(agora, zona)
    private val novembro = YearMonth.of(2026, 11)
    private val outubro = YearMonth.of(2026, 10)

    private val contaNubank = Conta(
        publicId = "conta-nubank",
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Nubank",
        tipo = TipoConta.CORRENTE,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val contaItau = Conta(
        publicId = "conta-itau",
        instituicao = InstituicaoFinanceira.ITAU,
        apelido = "Itaú",
        tipo = TipoConta.CORRENTE,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val cartaoNubank = Instrumento(
        publicId = "inst-nu",
        contaPublicId = contaNubank.publicId,
        tipo = TipoInstrumento.CREDITO,
        apelido = "Roxinho",
        ultimosDigitos = "1234",
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val cartaoItau = Instrumento(
        publicId = "inst-itau",
        contaPublicId = contaItau.publicId,
        tipo = TipoInstrumento.CREDITO,
        apelido = "Click",
        ultimosDigitos = "5678",
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val voucherCaju = Instrumento(
        publicId = "inst-caju",
        contaPublicId = contaItau.publicId,
        tipo = TipoInstrumento.VOUCHER,
        apelido = "Benefício",
        beneficio = BeneficioVoucher.VR,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    @Test
    fun `carrega relatorio mensal com categorias sem base e evolucao de seis meses`() = runTest {
        val relatorios = FakeRelatoriosProjection(
            mapOf(
                novembro to relatorioPreenchido(novembro),
                outubro to relatorioVazio(outubro),
            ),
        )
        val viewModel = criarViewModel(relatoriosProjection = relatorios)
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertFalse(estado.carregando)
        assertFalse(estado.falha)
        assertFalse(estado.mesVazio)
        assertEquals("Novembro 2026", estado.rotuloMes)
        assertEquals("outubro", estado.rotuloMesAnterior)
        assertEquals("R$ 1.500,00", estado.totalDespesasFormatado)
        assertEquals(2, estado.fatiasDonut.size)
        assertEquals(2, estado.categorias.size)
        assertEquals(6, estado.serieSeisMeses.size)
        assertEquals("sem base", estado.comparativos[1].row.badgeText)
        assertEquals("20%", estado.comparativos[0].row.badgeText)
    }

    @Test
    fun `troca de mes observa novo relatorio e exibe estado vazio quando sem dados`() = runTest {
        val relatorios = FakeRelatoriosProjection(
            mapOf(
                novembro to relatorioPreenchido(novembro),
                outubro to relatorioVazio(outubro),
            ),
        )
        val viewModel = criarViewModel(relatoriosProjection = relatorios)
        advanceUntilIdle()

        viewModel.onEvent(ReportsEvent.MesAnterior)
        advanceUntilIdle()

        val estadoOutubro = viewModel.uiState.value
        assertEquals(outubro, estadoOutubro.mes)
        assertTrue(estadoOutubro.mesVazio)
        assertTrue(estadoOutubro.podeAvancarMes)

        viewModel.onEvent(ReportsEvent.MesProximo)
        advanceUntilIdle()

        val estadoNovembro = viewModel.uiState.value
        assertEquals(novembro, estadoNovembro.mes)
        assertFalse(estadoNovembro.mesVazio)
        assertFalse(estadoNovembro.podeAvancarMes)
    }

    @Test
    fun `assinaturas distinguem mesmo estabelecimento em cartoes diferentes e calculam total`() = runTest {
        val sugestaoNu = sugestaoTeste(cartaoNubank.publicId, "spotify", "Spotify", 2190L, LocalDate.of(2026, 11, 2))
        val sugestaoItau = sugestaoTeste(cartaoItau.publicId, "spotify", "Spotify", 2190L, LocalDate.of(2026, 11, 5))
        val confirmada = assinaturaConfirmadaTeste("ass-1", "Netflix", 4490L, cartaoNubank.publicId)
        val confirmadaVoucher = assinaturaConfirmadaTeste("ass-2", "Caju Clube", 1000L, voucherCaju.publicId)
        val assinaturaRepo = FakeAssinaturaRepository(
            confirmadas = listOf(confirmada, confirmadaVoucher),
            sugestoes = listOf(sugestaoNu, sugestaoItau),
        )
        val viewModel = criarViewModel(assinaturaRepository = assinaturaRepo)
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(2, estado.sugestoesAssinatura.size)
        val instNu = estado.sugestoesAssinatura.first { it.chave.instrumentoPublicId == cartaoNubank.publicId }.card.instrumentName
        val instItau = estado.sugestoesAssinatura.first { it.chave.instrumentoPublicId == cartaoItau.publicId }.card.instrumentName
        assertTrue(instNu.contains("Nubank") && instNu.contains("1234"))
        assertTrue(instItau.contains("Itaú") && instItau.contains("5678"))
        assertEquals(5490L, estado.totalMensalAssinaturasCentavos)
        assertEquals("R$ 54,90", estado.totalMensalAssinaturasFormatado)
        val sugestaoNuCard = estado.sugestoesAssinatura.first { it.chave.instrumentoPublicId == cartaoNubank.publicId }.card
        assertEquals("Próxima cobrança: 02/12", sugestaoNuCard.nextChargeLabel)
        val confirmadaNetflix = estado.assinaturasConfirmadas.first { it.estabelecimento == "Netflix" }.row
        assertFalse(confirmadaNetflix.subtitle.contains("Próxima cobrança"))
        assertEquals("Próxima cobrança: 01/12", confirmadaNetflix.nextChargeLabel)
        assertTrue(estado.assinaturasConfirmadas.any { it.row.subtitle.contains("VR") })
    }

    @Test
    fun `confirmar e recusar sugestao possuem trava anti duplo clique`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val sugestao = SugestaoDeAssinatura(
            chave = ChaveAssinatura(cartaoNubank.publicId, "spotify"),
            estabelecimentoExibicao = "Spotify",
            valorReferenciaCentavos = 2190L,
            ultimaCobranca = LocalDate.of(2026, 11, 2),
            proximaCobrancaEstimada = LocalDate.of(2026, 12, 2),
            lancamentosQualificadoresPublicIds = listOf("l1", "l2"),
        )
        val assinaturaRepo = FakeAssinaturaRepository(
            sugestoes = listOf(sugestao),
            travaConfirmacao = gate,
        )
        val viewModel = criarViewModel(assinaturaRepository = assinaturaRepo)
        advanceUntilIdle()

        viewModel.onEvent(ReportsEvent.ConfirmarSugestao(sugestao.chave))
        viewModel.onEvent(ReportsEvent.ConfirmarSugestao(sugestao.chave))
        viewModel.onEvent(ReportsEvent.RecusarSugestao(sugestao.chave))
        runCurrent()
        assertTrue(chaveTextoDe(sugestao.chave) in viewModel.uiState.value.assinaturasEmProcessamento)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, assinaturaRepo.confirmacoesChamadas)
        assertEquals(0, assinaturaRepo.recusasChamadas)
        assertTrue(viewModel.uiState.value.assinaturasEmProcessamento.isEmpty())
    }

    @Test
    fun `cancelamento de assinatura abre dialogo e remove apos confirmacao`() = runTest {
        val confirmada = Assinatura(
            publicId = "ass-1",
            estabelecimento = "Netflix",
            estabelecimentoNormalizado = "netflix",
            valorReferenciaCentavos = 4490L,
            ultimaCobranca = LocalDate.of(2026, 11, 1),
            proximaCobrancaEstimada = LocalDate.of(2026, 12, 1),
            status = StatusAssinatura.CONFIRMADA,
            criadoEm = agora,
            atualizadoEm = agora,
            instrumentoPublicId = cartaoNubank.publicId,
        )
        val assinaturaRepo = FakeAssinaturaRepository(confirmadas = listOf(confirmada))
        val viewModel = criarViewModel(assinaturaRepository = assinaturaRepo)
        advanceUntilIdle()

        viewModel.onEvent(ReportsEvent.SolicitarCancelamentoAssinatura("ass-1"))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.confirmacaoCancelamento)

        viewModel.onEvent(ReportsEvent.ConfirmarCancelamentoAssinatura)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.confirmacaoCancelamento)
        assertEquals(listOf("ass-1"), assinaturaRepo.cancelamentosChamados)
        assertTrue(viewModel.uiState.value.assinaturasConfirmadas.isEmpty())
    }

    @Test
    fun `falha de leitura exibe estado discreto de erro e recupera ao tentar de novo`() = runTest {
        val relatorios = FakeRelatoriosProjection(
            dados = mapOf(novembro to relatorioPreenchido(novembro)),
            falharNaPrimeira = true,
        )
        val viewModel = criarViewModel(relatoriosProjection = relatorios)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.falha)
        assertFalse(viewModel.uiState.value.carregando)

        viewModel.onEvent(ReportsEvent.TentarDeNovo)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.falha)
        assertFalse(viewModel.uiState.value.carregando)
        assertEquals("R$ 1.500,00", viewModel.uiState.value.totalDespesasFormatado)
    }

    @Test
    fun `mes inicial usa fuso sao paulo mesmo quando clock do dispositivo esta em utc`() = runTest {
        val viradaUtcClock = Clock.fixed(Instant.parse("2026-11-01T01:30:00Z"), ZoneOffset.UTC)
        val relatorios = FakeRelatoriosProjection(mapOf(outubro to relatorioPreenchido(outubro)))
        val viewModel = ReportsViewModel(
            relatoriosProjection = relatorios,
            assinaturaRepository = FakeAssinaturaRepository(),
            instrumentoRepository = FakeInstrumentoRepo(listOf(cartaoNubank)),
            contaRepository = FakeContaRepo(listOf(contaNubank)),
            dispatchers = FakeAppDispatchers(dispatcherRule.testDispatcher),
            clock = viradaUtcClock,
        )
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(outubro, estado.mes)
        assertFalse(estado.podeAvancarMes)
    }

    @Test
    fun `mapeamento com seiscentos lancamentos conclui rapidamente dentro do orcamento`() = runTest {
        val lancamentos = gerarLancamentosLote(650)
        val tempoMs = measureTimeMillis {
            val agregado = AgregadorDeRelatorioMensal.agregar(novembro, lancamentos)
            val detalhado = detalhadoDe(novembro, agregado)
            val estado = mapearEstadoRelatorio(
                mes = novembro,
                mesAtual = novembro,
                detalhado = detalhado,
                confirmadas = emptyList(),
                sugestoes = emptyList(),
                contas = listOf(contaNubank, contaItau),
                instrumentos = listOf(cartaoNubank, cartaoItau),
            )
            assertTrue(estado.categorias.isNotEmpty())
        }
        assertTrue("Tempo esperado <= 1500ms, obtido ${tempoMs}ms", tempoMs <= 1500L)
    }

    @Test
    fun `sugestao originada pela categoria assinaturas exibe selo especifico`() {
        val sugestao = SugestaoDeAssinatura(
            chave = ChaveAssinatura(cartaoNubank.publicId, "netflix"),
            estabelecimentoExibicao = "Netflix",
            valorReferenciaCentavos = 4490L,
            ultimaCobranca = LocalDate.of(2026, 11, 5),
            proximaCobrancaEstimada = LocalDate.of(2026, 12, 5),
            lancamentosQualificadoresPublicIds = listOf("l-1"),
            porCategoria = true,
        )

        val estado = mapearEstadoRelatorio(
            mes = novembro,
            mesAtual = novembro,
            detalhado = relatorioVazio(novembro),
            confirmadas = emptyList(),
            sugestoes = listOf(sugestao),
            contas = listOf(contaNubank),
            instrumentos = listOf(cartaoNubank),
        )

        assertEquals("Categoria Assinaturas · Mensal", estado.sugestoesAssinatura.single().card.tagLabel)
    }

    private fun gerarLancamentosLote(quantidade: Int): List<Lancamento> {
        val categorias = listOf("cat-alim", "cat-merc", "cat-transp", "cat-lazer", "cat-saude", "cat-outros")
        return (0 until quantidade).map { indice ->
            val dia = (indice % 28) + 1
            val mesRef = if (indice % 5 == 0) outubro else novembro
            Lancamento(
                publicId = "l-$indice",
                instrumentoPublicId = cartaoNubank.publicId,
                categoriaPublicId = categorias[indice % categorias.size],
                valorCentavos = 1500L + (indice * 10L),
                dataHora = mesRef.atDay(dia).atTime(12, 0).atZone(zona).toInstant(),
                tipo = if (indice % 12 == 0) TipoLancamento.RECEITA else TipoLancamento.DESPESA,
                estabelecimento = "Loja $indice",
                status = StatusLancamento.CONFIRMADO,
                origem = OrigemLancamento.NOTIFICACAO,
                confianca = 1.0,
                criadoEm = agora,
                atualizadoEm = agora,
            )
        }
    }

    private fun detalhadoDe(mes: YearMonth, agregado: RelatorioMensal): RelatorioMensalDetalhado = RelatorioMensalDetalhado(
        relatorio = agregado,
        categorias = agregado.categoriasDespesa.map { item ->
            ResumoCategoriaRelatorio(
                categoriaPublicId = item.categoriaPublicId,
                nomeCategoria = item.categoriaPublicId ?: "Outros",
                valorCentavos = item.valorCentavos,
                valorMesAnteriorCentavos = item.valorMesAnteriorCentavos,
                percentualInteiro = item.percentualInteiro,
                pontosBase = item.pontosBase,
                variacaoMesAnterior = item.variacaoMesAnterior,
            )
        },
        mesAtual = mes,
    )

    private fun sugestaoTeste(
        instrumentoId: String,
        normalizado: String,
        exibicao: String,
        valorCentavos: Long,
        ultima: LocalDate,
    ): SugestaoDeAssinatura = SugestaoDeAssinatura(
        chave = ChaveAssinatura(instrumentoId, normalizado),
        estabelecimentoExibicao = exibicao,
        valorReferenciaCentavos = valorCentavos,
        ultimaCobranca = ultima,
        proximaCobrancaEstimada = ultima.plusMonths(1),
        lancamentosQualificadoresPublicIds = listOf("l1", "l2"),
    )

    private fun assinaturaConfirmadaTeste(
        publicId: String,
        estabelecimento: String,
        valorCentavos: Long,
        instrumentoId: String,
    ): Assinatura = Assinatura(
        publicId = publicId,
        estabelecimento = estabelecimento,
        estabelecimentoNormalizado = estabelecimento.lowercase(),
        valorReferenciaCentavos = valorCentavos,
        ultimaCobranca = LocalDate.of(2026, 11, 1),
        proximaCobrancaEstimada = LocalDate.of(2026, 12, 1),
        status = StatusAssinatura.CONFIRMADA,
        criadoEm = agora,
        atualizadoEm = agora,
        instrumentoPublicId = instrumentoId,
    )

    private fun criarViewModel(
        relatoriosProjection: RelatoriosProjection = FakeRelatoriosProjection(mapOf(novembro to relatorioPreenchido(novembro))),
        assinaturaRepository: AssinaturaRepository = FakeAssinaturaRepository(),
    ): ReportsViewModel = ReportsViewModel(
        relatoriosProjection = relatoriosProjection,
        assinaturaRepository = assinaturaRepository,
        instrumentoRepository = FakeInstrumentoRepo(listOf(cartaoNubank, cartaoItau, voucherCaju)),
        contaRepository = FakeContaRepo(listOf(contaNubank, contaItau)),
        dispatchers = FakeAppDispatchers(dispatcherRule.testDispatcher),
        clock = clock,
    )

    private fun relatorioPreenchido(mes: YearMonth): RelatorioMensalDetalhado {
        val serie = (5L downTo 0L).map { deslocamento ->
            val pontoMes = mes.minusMonths(deslocamento)
            PontoSerieMensal(pontoMes, 500_000L, if (pontoMes == mes) 150_000L else 125_000L)
        }
        val categorias = listOf(
            ResumoCategoriaRelatorio("cat-alim", "Alimentação", 100_000L, 83_333L, 67, 6700, VariacaoCategoria.Percentual(20)),
            ResumoCategoriaRelatorio("cat-lazer", "Lazer", 50_000L, 0L, 33, 3300, VariacaoCategoria.SemBase),
        )
        return RelatorioMensalDetalhado(
            relatorio = RelatorioMensal(mes, 500_000L, 150_000L, 350_000L, emptyList(), serie),
            categorias = categorias,
            mesAtual = mes,
        )
    }

    private fun relatorioVazio(mes: YearMonth): RelatorioMensalDetalhado {
        val serie = (5L downTo 0L).map { deslocamento ->
            PontoSerieMensal(mes = mes.minusMonths(deslocamento), receitasCentavos = 0L, despesasCentavos = 0L)
        }
        return RelatorioMensalDetalhado(
            relatorio = RelatorioMensal(
                mes = mes,
                totalReceitasCentavos = 0L,
                totalDespesasCentavos = 0L,
                saldoMensalCentavos = 0L,
                categoriasDespesa = emptyList(),
                serieSeisMeses = serie,
            ),
            categorias = emptyList(),
            mesAtual = mes,
        )
    }

    private class FakeRelatoriosProjection(
        private val dados: Map<YearMonth, RelatorioMensalDetalhado>,
        private var falharNaPrimeira: Boolean = false,
    ) : RelatoriosProjection {
        override fun observeRelatorioMensal(mes: YearMonth): Flow<RelatorioMensalDetalhado> = flow {
            if (falharNaPrimeira) {
                falharNaPrimeira = false
                error("falha simulada")
            }
            emit(dados.getValue(mes))
        }
    }

    private class FakeAssinaturaRepository(
        confirmadas: List<Assinatura> = emptyList(),
        sugestoes: List<SugestaoDeAssinatura> = emptyList(),
        private val travaConfirmacao: CompletableDeferred<Unit>? = null,
    ) : AssinaturaRepository {
        private val confirmadasState = MutableStateFlow(confirmadas)
        private val sugestoesState = MutableStateFlow(sugestoes)

        var confirmacoesChamadas: Int = 0
        var recusasChamadas: Int = 0
        val cancelamentosChamados = mutableListOf<String>()

        override fun observeAssinaturas(): Flow<List<Assinatura>> = confirmadasState

        override fun observeConfirmadas(): Flow<List<Assinatura>> = confirmadasState

        override fun observeSugestoes(): Flow<List<SugestaoDeAssinatura>> = sugestoesState

        override suspend fun buscar(publicId: String): Assinatura? = confirmadasState.value.firstOrNull { it.publicId == publicId }

        override suspend fun salvar(assinatura: Assinatura) = Unit

        override suspend fun salvarTodas(assinaturas: List<Assinatura>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit

        override suspend fun confirmarSugestao(sugestao: SugestaoDeAssinatura): Assinatura {
            confirmacoesChamadas++
            travaConfirmacao?.await()
            val criada = Assinatura(
                publicId = "ass-confirmada-$confirmacoesChamadas",
                estabelecimento = sugestao.estabelecimentoExibicao,
                estabelecimentoNormalizado = sugestao.chave.estabelecimentoNormalizado,
                valorReferenciaCentavos = sugestao.valorReferenciaCentavos,
                ultimaCobranca = sugestao.ultimaCobranca,
                proximaCobrancaEstimada = sugestao.proximaCobrancaEstimada,
                status = StatusAssinatura.CONFIRMADA,
                criadoEm = Instant.EPOCH,
                atualizadoEm = Instant.EPOCH,
                instrumentoPublicId = sugestao.chave.instrumentoPublicId,
            )
            sugestoesState.value = sugestoesState.value.filterNot { it.chave == sugestao.chave }
            confirmadasState.value = confirmadasState.value + criada
            return criada
        }

        override suspend fun recusarSugestao(sugestao: SugestaoDeAssinatura): Assinatura {
            recusasChamadas++
            sugestoesState.value = sugestoesState.value.filterNot { it.chave == sugestao.chave }
            return Assinatura(
                publicId = "ass-recusada",
                estabelecimento = sugestao.estabelecimentoExibicao,
                estabelecimentoNormalizado = sugestao.chave.estabelecimentoNormalizado,
                valorReferenciaCentavos = sugestao.valorReferenciaCentavos,
                status = StatusAssinatura.RECUSADA,
                criadoEm = Instant.EPOCH,
                atualizadoEm = Instant.EPOCH,
                instrumentoPublicId = sugestao.chave.instrumentoPublicId,
            )
        }

        override suspend fun cancelarAssinatura(assinaturaPublicId: String) {
            cancelamentosChamados += assinaturaPublicId
            confirmadasState.value = confirmadasState.value.filterNot { it.publicId == assinaturaPublicId }
        }
    }

    private class FakeContaRepo(contas: List<Conta>) : ContaRepository {
        private val fluxo = MutableStateFlow(contas)

        override fun observeContas(): Flow<List<Conta>> = fluxo

        override suspend fun buscar(publicId: String): Conta? = fluxo.value.firstOrNull { it.publicId == publicId }

        override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant, publicIds: List<String>) =
            emptyList<CadastroSalvo>()

        override suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo = error("nao usado")

        override suspend fun salvar(conta: Conta) = Unit

        override suspend fun salvarTodas(contas: List<Conta>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit

        override suspend fun arquivar(publicId: String, quando: Instant) = Unit

        override suspend fun reativar(publicId: String, quando: Instant) = Unit
    }

    private class FakeInstrumentoRepo(instrumentos: List<Instrumento>) : InstrumentoRepository {
        private val fluxo = MutableStateFlow(instrumentos)

        override fun observeInstrumentos(): Flow<List<Instrumento>> = fluxo

        override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
            fluxo.map { lista -> lista.filter { it.contaPublicId == contaPublicId } }

        override suspend fun buscar(publicId: String): Instrumento? = fluxo.value.firstOrNull { it.publicId == publicId }

        override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> = fluxo.value

        override suspend fun salvar(instrumento: Instrumento) = Unit

        override suspend fun salvarTodos(instrumentos: List<Instrumento>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit

        override suspend fun arquivar(publicId: String, quando: Instant) = Unit

        override suspend fun reativar(publicId: String, quando: Instant) = Unit
    }
}
