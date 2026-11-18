package com.pingo.feature.accounts.impl

import com.pingo.core.data.fatura.ResultadoCompletarDatasCartao
import com.pingo.core.data.voucher.ResultadoAjusteVoucher
import com.pingo.core.data.voucher.ValorAjusteSaldoVoucher
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.organizacao.ContaComIndicadores
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.model.organizacao.ResumoContasFinanceiro
import com.pingo.core.model.voucher.AjusteSaldoCalculado
import com.pingo.core.model.voucher.UltimaRecargaVoucher
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.ZoneOffset

class AccountsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val contas = FakeContaRepository()
    private val instrumentos = FakeInstrumentoRepository()
    private val projection = FakeContasFinanceiroProjection()
    private val clock: Clock = Clock.fixed(INSTANTE, ZoneOffset.UTC)

    @Test
    fun `sem contas mostra estado vazio`() {
        val viewModel = AccountsViewModel(contas, instrumentos)

        assertFalse(viewModel.uiState.value.carregando)
        assertFalse(viewModel.uiState.value.falha)
        assertTrue(viewModel.uiState.value.vazia)
    }

    @Test
    fun `contas ativas aparecem agrupadas por instituicao com seus instrumentos`() {
        contas.contas.value = listOf(
            conta("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank"),
            conta("conta-itau", InstituicaoFinanceira.ITAU, "Itaú"),
            conta("conta-arquivada", InstituicaoFinanceira.NUBANK, "Antiga", arquivada = true),
        )
        instrumentos.instrumentos.value = listOf(
            instrumento("nu-credito", "conta-nu", TipoInstrumento.CREDITO, "Cartão", arquivado = false),
            instrumento("nu-arquivado", "conta-nu", TipoInstrumento.DEBITO, "Débito antigo", arquivado = true),
            instrumento("itau-debito", "conta-itau", TipoInstrumento.DEBITO, "Débito", arquivado = false),
        )

        val viewModel = AccountsViewModel(contas, instrumentos)
        val grupos = viewModel.uiState.value.grupos

        assertEquals(listOf(InstituicaoFinanceira.NUBANK, InstituicaoFinanceira.ITAU), grupos.map { it.instituicao })
        assertEquals(listOf("Cartão"), grupos[0].contas.single().instrumentos.map { it.apelido })
        assertEquals(listOf("Débito"), grupos[1].contas.single().instrumentos.map { it.apelido })
        assertFalse(viewModel.uiState.value.vazia)
    }

    @Test
    fun `conta arquivada aparece na secao propria com seus instrumentos`() {
        contas.contas.value = listOf(
            conta("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank"),
            conta("conta-antiga", InstituicaoFinanceira.INTER, "Inter antiga", arquivada = true),
        )
        instrumentos.instrumentos.value = listOf(
            instrumento("nu-credito", "conta-nu", TipoInstrumento.CREDITO, "Cartão", arquivado = false),
            instrumento("antigo-credito", "conta-antiga", TipoInstrumento.CREDITO, "Cartão antigo", arquivado = true),
            instrumento("antigo-debito", "conta-antiga", TipoInstrumento.DEBITO, "Débito antigo", arquivado = false),
        )

        val viewModel = AccountsViewModel(contas, instrumentos)

        assertEquals(
            listOf("conta-nu"),
            viewModel.uiState.value.grupos.flatMap { grupo -> grupo.contas.map { it.conta.publicId } },
        )
        val arquivada = viewModel.uiState.value.arquivadas.single()
        assertEquals("conta-antiga", arquivada.conta.publicId)
        assertEquals(listOf("Cartão antigo", "Débito antigo"), arquivada.instrumentos.map { it.apelido })
        assertFalse(viewModel.uiState.value.vazia)
    }

    @Test
    fun `so contas arquivadas nao mostram o estado vazio de primeiro cadastro`() {
        contas.contas.value = listOf(conta("conta-antiga", InstituicaoFinanceira.NUBANK, "Antiga", arquivada = true))
        instrumentos.instrumentos.value = listOf(
            instrumento("antigo", "conta-antiga", TipoInstrumento.DEBITO, "Débito", arquivado = true),
        )

        val viewModel = AccountsViewModel(contas, instrumentos)

        assertTrue(viewModel.uiState.value.grupos.isEmpty())
        assertEquals(1, viewModel.uiState.value.arquivadas.size)
        assertFalse(viewModel.uiState.value.vazia)
    }

    @Test
    fun `falha de leitura mostra erro e tentar de novo recarrega`() {
        contas.falhaAoObservar = IllegalStateException("banco indisponível")

        val viewModel = AccountsViewModel(contas, instrumentos)

        assertTrue(viewModel.uiState.value.falha)
        assertFalse(viewModel.uiState.value.carregando)
        assertFalse(viewModel.uiState.value.vazia)

        contas.falhaAoObservar = null
        contas.contas.value = listOf(conta("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank"))
        viewModel.tentarDeNovo()

        assertFalse(viewModel.uiState.value.falha)
        assertEquals(1, viewModel.uiState.value.grupos.size)
    }

    @Test
    fun `projecao financeira exibe indicadores e materializa recargas com seguranca mesmo se falhar`() {
        projection.resumo.value = criarResumoFinanceiroDeTeste()
        var materializacoes = 0

        val viewModel = criarViewModelF2(
            materializarRecargas = {
                materializacoes++
                error("falha transitória")
            },
        )

        assertEquals(1, materializacoes)
        assertFalse(viewModel.uiState.value.falha)
        assertEquals(2, viewModel.uiState.value.grupos.size)
        val indicadoresNu = viewModel.uiState.value.grupos[0].contas.single().indicadores
        assertEquals(2, indicadoresNu.size)
        val faturaCredito = (indicadoresNu[0] as IndicadorDeInstrumento.Credito).estadoFatura
        assertEquals(128_450L, (faturaCredito as EstadoFaturaCartao.Configurada).totalCentavos)
        assertEquals(102_000L, (indicadoresNu[1] as IndicadorDeInstrumento.Debito).gastoDoMesCentavos)

        viewModel.atualizar()
        assertEquals(2, materializacoes)
    }

    private fun criarResumoFinanceiroDeTeste(): ResumoContasFinanceiro {
        val contaNu = conta("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank pessoal")
        val contaCaju = conta("conta-caju", InstituicaoFinanceira.CAJU, "Caju", tipo = TipoConta.BENEFICIO)
        return ResumoContasFinanceiro(
            contasAtivas = listOf(
                contaNubankComIndicadores(contaNu),
                contaCajuComIndicadores(contaCaju),
            ),
            contasArquivadas = emptyList(),
        )
    }

    private fun contaNubankComIndicadores(contaNu: Conta): ContaComIndicadores {
        val instCredito = instrumento("inst-cred", "conta-nu", TipoInstrumento.CREDITO, "Cartão", arquivado = false)
        val instDebito = instrumento("inst-deb", "conta-nu", TipoInstrumento.DEBITO, "Conta", arquivado = false)
        return ContaComIndicadores(
            conta = contaNu,
            instrumentos = listOf(
                IndicadorDeInstrumento.Credito(
                    instrumento = instCredito,
                    estadoFatura = EstadoFaturaCartao.Configurada(
                        faturaPublicId = "fat-1",
                        competencia = LocalDate.of(2026, 11, 1),
                        dataFechamento = LocalDate.of(2026, 11, 3),
                        dataVencimento = LocalDate.of(2026, 11, 10),
                        totalCentavos = 128_450L,
                        status = StatusFatura.ABERTA,
                        diasParaVencimento = 19L,
                        vencida = false,
                    ),
                ),
                IndicadorDeInstrumento.Debito(
                    instrumento = instDebito,
                    gastoDoMesCentavos = 102_000L,
                    competenciaMes = YearMonth.of(2026, 11),
                ),
            ),
        )
    }

    private fun contaCajuComIndicadores(contaCaju: Conta): ContaComIndicadores {
        val instVoucher = instrumento(
            publicId = "inst-vr",
            contaPublicId = "conta-caju",
            tipo = TipoInstrumento.VOUCHER,
            apelido = "Refeição",
            arquivado = false,
            beneficio = BeneficioVoucher.VR,
        )
        return ContaComIndicadores(
            conta = contaCaju,
            instrumentos = listOf(
                IndicadorDeInstrumento.Voucher(
                    instrumento = instVoucher,
                    beneficio = BeneficioVoucher.VR,
                    saldoCentavos = 48_620L,
                    ultimaRecarga = UltimaRecargaVoucher(LocalDate.of(2026, 11, 1), 80_000L, prevista = false),
                ),
            ),
        )
    }

    @Test
    fun `completar datas do cartao valida limites 1 a 31 e fecha modal ao salvar`() {
        var chamada: Triple<String, Int, Int>? = null
        val inst = instrumento("inst-cred", "conta-nu", TipoInstrumento.CREDITO, "Cartão", arquivado = false)
        val viewModel = criarViewModelF2(
            completarDatas = { id, fechamento, vencimento ->
                chamada = Triple(id, fechamento, vencimento)
                ResultadoCompletarDatasCartao.Sucesso(inst.copy(fechamentoDia = fechamento, vencimentoDia = vencimento))
            },
        )

        viewModel.onEvent(AccountsEvent.AbrirCompletarDatas("inst-cred", "Crédito · Cartão"))
        viewModel.onEvent(AccountsEvent.AlterarFechamentoModal("0"))
        viewModel.onEvent(AccountsEvent.AlterarVencimentoModal("32"))
        assertFalse(viewModel.uiState.value.modalCompletarDatas?.podeSalvar == true)

        viewModel.onEvent(AccountsEvent.AlterarFechamentoModal("5"))
        viewModel.onEvent(AccountsEvent.AlterarVencimentoModal("12"))
        assertTrue(viewModel.uiState.value.modalCompletarDatas?.podeSalvar == true)

        viewModel.onEvent(AccountsEvent.SalvarDatasCartao)

        assertEquals(Triple("inst-cred", 5, 12), chamada)
        assertNull(viewModel.uiState.value.modalCompletarDatas)
    }

    @Test
    fun `completar datas com erro preserva modal aberto e exibe mensagem`() {
        val viewModel = criarViewModelF2(
            completarDatas = { _, _, _ -> ResultadoCompletarDatasCartao.DadosInvalidos },
        )

        viewModel.onEvent(AccountsEvent.AbrirCompletarDatas("inst-cred", "Cartão"))
        viewModel.onEvent(AccountsEvent.AlterarFechamentoModal("10"))
        viewModel.onEvent(AccountsEvent.AlterarVencimentoModal("15"))
        viewModel.onEvent(AccountsEvent.SalvarDatasCartao)

        val modal = viewModel.uiState.value.modalCompletarDatas
        assertNotNull(modal)
        assertFalse(modal!!.salvando)
        assertEquals("Informe dias válidos entre 1 e 31.", modal.erro)
    }

    @Test
    fun `ajustar saldo voucher por saldo alvo e delta envia parametro correto`() {
        val chamadas = mutableListOf<Triple<String, ValorAjusteSaldoVoucher, String?>>()
        val viewModel = criarViewModelF2(
            ajustarSaldo = { id, entrada, obs ->
                chamadas.add(Triple(id, entrada, obs))
                ResultadoAjusteVoucher.Aplicado(
                    instrumentoPublicId = id,
                    ajuste = AjusteSaldoCalculado(10_000L, 25_000L, 15_000L, 15_000L),
                    lancamentoPublicId = "lanc-1",
                    referenciaPublicId = "ref-1",
                )
            },
        )

        viewModel.onEvent(AccountsEvent.AbrirAjustarSaldo("inst-vr", "VR · Refeição", 10_000L))
        viewModel.onEvent(AccountsEvent.AlterarValorAjuste("250,00"))
        viewModel.onEvent(AccountsEvent.AlterarObservacaoAjuste("Acerto manual"))
        viewModel.onEvent(AccountsEvent.SalvarAjusteVoucher)

        assertEquals(
            Triple("inst-vr", ValorAjusteSaldoVoucher.SaldoAlvo(25_000L), "Acerto manual"),
            chamadas[0],
        )
        assertNull(viewModel.uiState.value.modalAjustarSaldo)

        viewModel.onEvent(AccountsEvent.AbrirAjustarSaldo("inst-vr", "VR · Refeição", 25_000L))
        viewModel.onEvent(AccountsEvent.AlterarModoAjuste(ModoAjusteVoucher.DELTA))
        viewModel.onEvent(AccountsEvent.AlterarValorAjuste("-20,50"))
        viewModel.onEvent(AccountsEvent.SalvarAjusteVoucher)

        assertEquals(
            Triple("inst-vr", ValorAjusteSaldoVoucher.Delta(-2_050L), ""),
            chamadas[1],
        )
    }

    @Test
    fun `ajustar saldo voucher negativo retorna erro no modal`() {
        val viewModel = criarViewModelF2(
            ajustarSaldo = { _, _, _ -> ResultadoAjusteVoucher.ValorInvalido },
        )

        viewModel.onEvent(AccountsEvent.AbrirAjustarSaldo("inst-vr", "VR · Refeição", 1_000L))
        viewModel.onEvent(AccountsEvent.AlterarModoAjuste(ModoAjusteVoucher.DELTA))
        viewModel.onEvent(AccountsEvent.AlterarValorAjuste("-50,00"))
        viewModel.onEvent(AccountsEvent.SalvarAjusteVoucher)

        val modal = viewModel.uiState.value.modalAjustarSaldo
        assertNotNull(modal)
        assertFalse(modal!!.salvando)
        assertEquals("O saldo final não pode ficar negativo.", modal.erro)
    }

    private fun criarViewModelF2(
        completarDatas: suspend (String, Int, Int) -> ResultadoCompletarDatasCartao = { _, _, _ ->
            ResultadoCompletarDatasCartao.NaoEncontrado
        },
        ajustarSaldo: suspend (String, ValorAjusteSaldoVoucher, String?) -> ResultadoAjusteVoucher = { _, _, _ ->
            ResultadoAjusteVoucher.NaoEncontrado
        },
        materializarRecargas: suspend () -> Int = { 0 },
    ): AccountsViewModel = AccountsViewModel(
        contasFinanceiroProjection = projection,
        completarDatas = completarDatas,
        ajustarSaldo = ajustarSaldo,
        materializarRecargas = materializarRecargas,
        clock = clock,
    )

    private fun conta(
        publicId: String,
        instituicao: InstituicaoFinanceira,
        apelido: String,
        arquivada: Boolean = false,
        tipo: TipoConta = TipoConta.CORRENTE,
    ): Conta = Conta(
        publicId = publicId,
        instituicao = instituicao,
        apelido = apelido,
        tipo = tipo,
        arquivada = arquivada,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private fun instrumento(
        publicId: String,
        contaPublicId: String,
        tipo: TipoInstrumento,
        apelido: String,
        arquivado: Boolean,
        beneficio: BeneficioVoucher? = null,
    ): Instrumento = Instrumento(
        publicId = publicId,
        contaPublicId = contaPublicId,
        tipo = tipo,
        apelido = apelido,
        arquivado = arquivado,
        beneficio = beneficio,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-11-10T12:00:00Z")
    }
}
