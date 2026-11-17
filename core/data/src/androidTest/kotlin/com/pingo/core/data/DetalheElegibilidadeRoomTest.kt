package com.pingo.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.captura.CapturaParaProcessar
import com.pingo.core.data.captura.ResultadoProcessamento
import com.pingo.core.data.detalhe.CampoDoDetalhe
import com.pingo.core.data.detalhe.EdicaoDoLancamento
import com.pingo.core.data.detalhe.MotivoDaRecusa
import com.pingo.core.data.detalhe.ResultadoDaEdicao
import com.pingo.core.data.detalhe.ResultadoDaTransferencia
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetalheElegibilidadeRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var banco: BancoRoomDeTeste

    @Before
    fun preparaBanco() {
        banco = BancoRoomDeTeste(context, "detalhe-elegibilidade-room-test.db")
    }

    @After
    fun fechaBanco() {
        banco.fechar()
    }

    @Test
    fun editarRejeitaNovoVinculoAposArquivamentoMasPreservaHistoricoInalterado() = runBlocking {
        banco.semearCategoriasPadrao()
        banco.semearConta("inter", InstituicaoFinanceira.INTER, arquivada = true)
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearInstrumento("cartao-inter", "inter", TipoInstrumento.CREDITO, arquivado = true)
        banco.semearInstrumento("cartao-nubank", "nubank", TipoInstrumento.CREDITO)
        banco.semearLancamento("lanc-hist", "cartao-inter", valorCentavos = 9000, tipo = TipoLancamento.DESPESA)

        val (_, tentativaTroca) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.contas(tx).arquivar("nubank", banco.agora) },
            segundo = { tx ->
                banco.editor(tx).editar(edicao("lanc-hist", "cartao-nubank", 9000, "Mercado"))
            },
        )

        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.INSTRUMENTO)), tentativaTroca)
        assertEquals("cartao-inter", banco.lancamentoRepository.buscar("lanc-hist")?.instrumentoPublicId)

        val edicaoHistorica = banco.editor().editar(edicao("lanc-hist", "cartao-inter", 9500, "Farmacia"))
        val salvo = edicaoHistorica as ResultadoDaEdicao.Salvo
        assertEquals("cartao-inter", salvo.lancamento.instrumentoPublicId)
        assertEquals(9500L, salvo.lancamento.valorCentavos)
        assertEquals("Farmacia", salvo.lancamento.estabelecimento)
    }

    @Test
    fun marcarTransferenciaRejeitaParQuandoDestinoArquivaAntesDoCommit() = runBlocking {
        banco.semearCategoriasPadrao()
        banco.semearConta("nubank", InstituicaoFinanceira.NUBANK)
        banco.semearConta("itau", InstituicaoFinanceira.ITAU)
        banco.semearInstrumento("debito-nubank", "nubank", TipoInstrumento.DEBITO)
        banco.semearInstrumento("debito-itau", "itau", TipoInstrumento.DEBITO)
        banco.semearLancamento("despesa-nubank", "debito-nubank", valorCentavos = 12000, tipo = TipoLancamento.DESPESA)

        val (_, resultado) = banco.executarCorridaPrimeiroComita(
            primeiro = { tx -> banco.contas(tx).arquivar("itau", banco.agora) },
            segundo = { tx -> banco.marcador(tx).marcar("despesa-nubank", "debito-itau") },
        )

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.DESTINO_NAO_ENCONTRADO), resultado)
        assertNull(banco.lancamentoRepository.buscar("despesa-nubank")?.transferenciaId)
    }

    @Test
    fun instrumentoAtivoSobContaArquivadaLegadaEBloqueadoEmTodasOperacoes() = runBlocking {
        banco.semearCategoriasPadrao()
        banco.semearConta("legada", InstituicaoFinanceira.INTER, arquivada = true)
        banco.semearConta("ativa", InstituicaoFinanceira.NUBANK)
        banco.semearInstrumento("inst-legado", "legada", TipoInstrumento.CREDITO, digitos = "9999", arquivado = false)
        banco.semearInstrumento("inst-ativo", "ativa", TipoInstrumento.DEBITO)
        banco.semearLancamento("despesa-ativa", "inst-ativo", valorCentavos = 5000, tipo = TipoLancamento.DESPESA)

        val capturado = banco.processador().processar(
            CapturaParaProcessar(
                instituicao = InstituicaoFinanceira.INTER,
                transacao = TransacaoExtraida(
                    padraoId = "inter-compra",
                    tipo = TipoLancamentoPadrao.COMPRA,
                    direcao = DirecaoPadrao.DESPESA,
                    valorCentavos = 5000,
                    estabelecimento = "Loja",
                    digitos = "9999",
                ),
                postTime = banco.agora,
            ),
        ) as ResultadoProcessamento.Gravado
        assertNull(capturado.lancamento.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, capturado.lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, capturado.lancamento.motivoPendencia)

        val manual = banco.manual().registrar(
            NovoLancamentoManual(TipoLancamento.DESPESA, 5000, "inst-legado", dataHora = banco.agora, estabelecimento = "Loja"),
        )
        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.INSTRUMENTO)), manual)

        val edicao = banco.editor().editar(edicao("despesa-ativa", "inst-legado", 5000, "Loja"))
        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.INSTRUMENTO)), edicao)

        val transferencia = banco.marcador().marcar("despesa-ativa", "inst-legado")
        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.DESTINO_NAO_ENCONTRADO), transferencia)
    }

    private fun edicao(publicId: String, instrumentoPublicId: String, valorCentavos: Long, estabelecimento: String) = EdicaoDoLancamento(
        publicId = publicId,
        tipo = TipoLancamento.DESPESA,
        valorCentavos = valorCentavos,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = "cat-outros-despesa",
        dataHora = banco.agora,
        estabelecimento = estabelecimento,
    )
}
