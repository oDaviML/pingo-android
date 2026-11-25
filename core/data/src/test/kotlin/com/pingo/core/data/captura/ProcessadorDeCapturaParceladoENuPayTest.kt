package com.pingo.core.data.captura

import com.pingo.core.data.categoria.AmbienteCategorias
import com.pingo.core.data.impl.FakeTransacaoLocal
import com.pingo.core.data.impl.ProcessadorDeCapturaImpl
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.TipoInstrumentoPadrao
import com.pingo.core.parser.TipoLancamentoPadrao
import com.pingo.core.parser.TransacaoExtraida
import com.pingo.core.testing.FakeAppDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessadorDeCapturaParceladoENuPayTest {
    private val contas = FakeContas()
    private val instrumentos = FakeInstrumentos(contas)
    private val lancamentos = FakeLancamentos(instrumentos)
    private val transacoes = FakeTransacaoLocal()
    private val clock = MutableClock(AGORA_DA_CAPTURA, FUSO_DA_CAPTURA)

    @Test
    fun `NuPay sem digitos escolhe o cartao de credito cadastrado mesmo havendo debito`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val contaNubank = contas.criar(InstituicaoFinanceira.NUBANK, "Nubank")
        instrumentos.criarNaConta(contaNubank, tipo = TipoInstrumento.DEBITO, apelido = "Débito")
        val credito = instrumentos.criarNaConta(contaNubank, tipo = TipoInstrumento.CREDITO, apelido = "Crédito")

        val resultado = processador(categorias).processar(
            captura(
                instituicao = InstituicaoFinanceira.NUBANK,
                valorCentavos = 1089L,
                estabelecimento = "Uber",
                tipoInstrumento = TipoInstrumentoPadrao.CREDITO,
                textoBruto = "Pagamento com NuPay aprovado\nR$ 10,89 em 1x no crédito sem juros com NuPay APROVADO em Uber.",
            ),
        )

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(credito.publicId, lancamento.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        assertEquals(1, lancamento.totalParcelas)
    }

    @Test
    fun `compra parcelada capturada no credito cria todas as parcelas e evita duplicata`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val contaItau = contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        instrumentos.criarNaConta(contaItau, tipo = TipoInstrumento.DEBITO, apelido = "Débito")
        val credito = instrumentos.criarNaConta(contaItau, tipo = TipoInstrumento.CREDITO, apelido = "Crédito")
        val processador = processador(categorias)
        val notificacao = captura(
            instituicao = InstituicaoFinanceira.ITAU,
            valorCentavos = 30061L,
            estabelecimento = "HNA*OBOTICARIO",
            totalParcelas = 3,
            tipoInstrumento = TipoInstrumentoPadrao.CREDITO,
            textoBruto = "Olá! Compra parcelada aprovada de R$ 300,61 em 3x em HNA*OBOTICARIO",
        )

        val primeiro = processador.processar(notificacao)
        val segundo = processador.processar(notificacao)

        val pai = (primeiro as ResultadoProcessamento.Gravado).lancamento
        assertEquals(ResultadoProcessamento.Duplicada, segundo)
        assertEquals(credito.publicId, pai.instrumentoPublicId)
        assertEquals(1, pai.parcelaNumero)
        assertEquals(3, pai.totalParcelas)
        assertEquals(30061L, pai.valorTotalCompraCentavos)
        assertEquals(10021L, pai.valorCentavos)
        assertNotNull(pai.hashDuplicata)
        assertNotNull(pai.textoBruto)

        val parcelas = lancamentos.listarParcelasDaCompra(pai.publicId)
        assertEquals(3, parcelas.size)
        assertEquals(listOf(10021L, 10020L, 10020L), parcelas.map { it.valorCentavos })
        assertEquals(listOf(1, 2, 3), parcelas.map { it.parcelaNumero })
        assertTrue(parcelas.drop(1).all { it.compraPaiPublicId == pai.publicId && it.hashDuplicata == null })
    }

    @Test
    fun `compra parcelada sem instrumento unico grava lancamento unico pendente para revisao`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val contaItau = contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        instrumentos.criarNaConta(contaItau, tipo = TipoInstrumento.CREDITO, apelido = "Card 1")
        instrumentos.criarNaConta(contaItau, tipo = TipoInstrumento.CREDITO, apelido = "Card 2")

        val resultado = processador(categorias).processar(
            captura(
                instituicao = InstituicaoFinanceira.ITAU,
                valorCentavos = 30000L,
                estabelecimento = "HNA*OBOTICARIO",
                totalParcelas = 3,
                tipoInstrumento = TipoInstrumentoPadrao.CREDITO,
            ),
        )

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_AMBIGUO.name, lancamento.motivoPendencia)
        assertEquals(1, lancamento.totalParcelas)
        assertEquals(1, lancamentos.todos().size)
    }

    private fun TestScope.processador(categorias: AmbienteCategorias): ProcessadorDeCaptura = ProcessadorDeCapturaImpl(
        lancamentos,
        instrumentos,
        categorias.repositorio,
        categorias.aprendida,
        transacoes,
        clock,
        FakeAppDispatchers(StandardTestDispatcher(testScheduler)),
    )

    private fun captura(
        instituicao: InstituicaoFinanceira,
        valorCentavos: Long,
        estabelecimento: String,
        totalParcelas: Int = 1,
        tipoInstrumento: TipoInstrumentoPadrao? = null,
        textoBruto: String? = "Olá! Compra aprovada",
    ): CapturaParaProcessar = CapturaParaProcessar(
        instituicao = instituicao,
        transacao = TransacaoExtraida(
            padraoId = "padrao-de-teste",
            tipo = TipoLancamentoPadrao.COMPRA,
            direcao = DirecaoPadrao.DESPESA,
            valorCentavos = valorCentavos,
            estabelecimento = estabelecimento,
            digitos = null,
            moeda = "BRL",
            totalParcelas = totalParcelas,
            tipoInstrumento = tipoInstrumento,
        ),
        postTime = AGORA_DA_CAPTURA,
        textoBruto = textoBruto,
    )
}
