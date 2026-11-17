package com.pingo.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.captura.CapturaParaProcessar
import com.pingo.core.data.captura.ResultadoProcessamento
import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.data.manual.NovoLancamentoManual
import com.pingo.core.data.manual.ResultadoLancamentoManual
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.TipoLancamentoPadrao
import com.pingo.core.parser.TransacaoExtraida
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElegibilidadeTransacionalRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var banco: BancoRoomDeTeste

    @Before
    fun preparaBanco() {
        banco = BancoRoomDeTeste(context, "elegibilidade-transacional-room-test.db")
    }

    @After
    fun fechaBanco() {
        banco.fechar()
    }

    @Test
    fun capturaQuandoArquivamentoComitaAntesFicaPendenteSemVinculo() = runBlocking {
        banco.semearCategoriasPadrao()
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearInstrumento("cartao-nubank", "nubank", TipoInstrumento.CREDITO, digitos = "1234")

        val (_, resultado) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.contas(tx).arquivar("nubank", banco.agora) },
            segundo = { tx -> banco.processador(tx).processar(capturaNubank()) },
        )

        val gravado = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertNull(gravado.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, gravado.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, gravado.motivoPendencia)
        val salvo = banco.lancamentoRepository.buscar(gravado.publicId)
        assertNull(salvo?.instrumentoPublicId)
    }

    @Test
    fun capturaQuandoEscritaComitaAntesPreservaVinculoHistorico() = runBlocking {
        banco.semearCategoriasPadrao()
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearInstrumento("cartao-nubank", "nubank", TipoInstrumento.CREDITO, digitos = "1234")

        val (resultado, _) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.processador(tx).processar(capturaNubank()) },
            segundo = { tx -> banco.contas(tx).arquivar("nubank", banco.agora) },
        )

        val gravado = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals("cartao-nubank", gravado.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, gravado.status)
        assertTrue(banco.contas().buscar("nubank")?.arquivada == true)
        assertEquals("cartao-nubank", banco.lancamentoRepository.buscar(gravado.publicId)?.instrumentoPublicId)
    }

    @Test
    fun capturaTransferenciaQuandoDestinoArquivaAntesNaoPareia() = runBlocking {
        banco.semearCategoriasPadrao()
        banco.semearConta("itau", InstituicaoFinanceira.ITAU)
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearInstrumento("debito-itau", "itau", TipoInstrumento.DEBITO)
        banco.semearInstrumento("debito-nubank", "nubank", TipoInstrumento.DEBITO)
        banco.semearLancamento("receita-nubank", "debito-nubank", valorCentavos = 15000, tipo = TipoLancamento.RECEITA)

        val (_, resultado) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.contas(tx).arquivar("nubank", banco.agora) },
            segundo = { tx -> banco.processador(tx).processar(capturaPixItau()) },
        )

        val gravado = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals("debito-itau", gravado.instrumentoPublicId)
        assertEquals(TipoLancamento.DESPESA, gravado.tipo)
        assertNull(gravado.transferenciaId)
        assertNull(banco.lancamentoRepository.buscar("receita-nubank")?.transferenciaId)
    }

    @Test
    fun capturaTransferenciaQuandoEscritaComitaAntesPareiaAntesDoArquivamento() = runBlocking {
        banco.semearCategoriasPadrao()
        banco.semearConta("itau", InstituicaoFinanceira.ITAU)
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearInstrumento("debito-itau", "itau", TipoInstrumento.DEBITO)
        banco.semearInstrumento("debito-nubank", "nubank", TipoInstrumento.DEBITO)
        banco.semearLancamento("receita-nubank", "debito-nubank", valorCentavos = 15000, tipo = TipoLancamento.RECEITA)

        val (resultado, _) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.processador(tx).processar(capturaPixItau()) },
            segundo = { tx -> banco.contas(tx).arquivar("nubank", banco.agora) },
        )

        val gravado = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(TipoLancamento.TRANSFERENCIA, gravado.tipo)
        assertNotNull(gravado.transferenciaId)
        assertEquals(gravado.transferenciaId, banco.lancamentoRepository.buscar("receita-nubank")?.transferenciaId)
        assertTrue(banco.contas().buscar("nubank")?.arquivada == true)
    }

    @Test
    fun manualSimplesQuandoArquivamentoComitaAntesRejeitaSemGravar() = runBlocking {
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearInstrumento("cartao-nubank", "nubank", TipoInstrumento.CREDITO)

        val (_, resultado) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.contas(tx).arquivar("nubank", banco.agora) },
            segundo = { tx -> banco.manual(tx).registrar(novoManual("cartao-nubank")) },
        )

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.INSTRUMENTO)), resultado)
        assertTrue(banco.lancamentoRepository.observeLancamentos().first().isEmpty())
    }

    @Test
    fun manualTransferenciaQuandoDestinoArquivaAntesRejeitaParAtomico() = runBlocking {
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearConta("itau", InstituicaoFinanceira.ITAU)
        banco.semearInstrumento("cartao-nubank", "nubank", TipoInstrumento.DEBITO)
        banco.semearInstrumento("cartao-itau", "itau", TipoInstrumento.DEBITO)

        val (_, resultado) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.contas(tx).arquivar("itau", banco.agora) },
            segundo = { tx ->
                banco.manual(tx).registrar(novoManual("cartao-nubank", destino = "cartao-itau"))
            },
        )

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.DESTINO)), resultado)
        assertTrue(banco.lancamentoRepository.observeLancamentos().first().isEmpty())
    }

    @Test
    fun manualTransferenciaQuandoEscritaComitaAntesPreservaParHistorico() = runBlocking {
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearConta("itau", InstituicaoFinanceira.ITAU)
        banco.semearInstrumento("cartao-nubank", "nubank", TipoInstrumento.DEBITO)
        banco.semearInstrumento("cartao-itau", "itau", TipoInstrumento.DEBITO)

        val (resultado, _) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx ->
                banco.manual(tx).registrar(novoManual("cartao-nubank", destino = "cartao-itau"))
            },
            segundo = { tx -> banco.contas(tx).arquivar("itau", banco.agora) },
        )

        val salvo = resultado as ResultadoLancamentoManual.Salvo
        assertEquals(2, salvo.lancamentos.size)
        val persistidos = banco.lancamentoRepository.observeLancamentos().first()
        assertEquals(2, persistidos.size)
        assertEquals(setOf("cartao-nubank", "cartao-itau"), persistidos.mapNotNull { it.instrumentoPublicId }.toSet())
        assertTrue(banco.contas().buscar("itau")?.arquivada == true)
    }

    private fun capturaNubank() = CapturaParaProcessar(
        instituicao = InstituicaoFinanceira.NUBANK,
        transacao = TransacaoExtraida(
            padraoId = "nubank-compra",
            tipo = TipoLancamentoPadrao.COMPRA,
            direcao = DirecaoPadrao.DESPESA,
            valorCentavos = 4500,
            estabelecimento = "Padaria",
            digitos = "1234",
        ),
        postTime = banco.agora,
    )

    private fun capturaPixItau() = CapturaParaProcessar(
        instituicao = InstituicaoFinanceira.ITAU,
        transacao = TransacaoExtraida(
            padraoId = "itau-pix-enviado",
            tipo = TipoLancamentoPadrao.PIX_ENVIADO,
            direcao = DirecaoPadrao.DESPESA,
            valorCentavos = 15000,
            estabelecimento = "DAVI",
            digitos = null,
        ),
        postTime = banco.agora,
    )

    private fun novoManual(origem: String, destino: String? = null) = NovoLancamentoManual(
        tipo = if (destino == null) TipoLancamento.DESPESA else TipoLancamento.TRANSFERENCIA,
        valorCentavos = 8500,
        instrumentoPublicId = origem,
        destinoPublicId = destino,
        dataHora = banco.agora,
        estabelecimento = "Padaria",
    )
}
