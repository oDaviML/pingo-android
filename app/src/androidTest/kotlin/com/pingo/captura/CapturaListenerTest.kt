package com.pingo.captura

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.CapturaControllerImpl
import com.pingo.core.notifications.ConexaoDoListener
import com.pingo.core.notifications.EscopoDeCaptura
import com.pingo.core.notifications.NotificacoesDeCaptura
import com.pingo.core.notifications.RebindDeListenerDoSistema
import com.pingo.core.notifications.ServicoDeCapturaDoAndroid
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class CapturaListenerTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val dataSource: CapturaDataSource = CapturaDataSourceImpl(contexto)
    private val acesso = AcessoNotificacoesDoSistema(contexto)
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val controller: CapturaController = CapturaControllerImpl(
        capturaDataSource = dataSource,
        acessoNotificacoes = acesso,
        rebindDeListener = RebindDeListenerDoSistema(contexto),
        servicoDeCaptura = ServicoDeCapturaDoAndroid(contexto),
        conexaoDoListener = ConexaoDoListener(),
        escopoDeCaptura = EscopoDeCaptura(DefaultAppDispatchers),
        clock = Clock.systemDefaultZone(),
    )
    private val gerenciadorNotificacoes: NotificationManager = contexto.getSystemService(NotificationManager::class.java)

    @Before
    fun concedeAcessoEConectaOListener() {
        semearInstrumentoECategoria()
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        shell("cmd notification allow_listener $COMPONENTE")
        runBlocking { controller.reconectar() }
        aguardarEstado(EstadoListener.ATIVO)
        aguardarNotificacaoProcessada()
        aguardarContadoresEstaveis()
    }

    @Test
    fun listenerRodaEmForegroundServiceComNotificacaoPersistente() {
        val persistente = aguardarNotificacaoPersistente()

        assertTrue(persistente.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(persistente.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0)
        assertEquals(Notification.CATEGORY_SERVICE, persistente.notification.category)
        assertTrue(acesso.concedido())
    }

    @Test
    fun acessoRevogadoMarcaParadoComMotivoSemCrash() {
        shell("cmd notification disallow_listener $COMPONENTE")

        val estado = aguardarEstado(EstadoListener.PARADO, MotivoParada.PERMISSAO_REVOGADA)
        aguardarAcessoRevogado()

        assertFalse(acesso.concedido())
        assertNotNull(estado.atualizadoEm)
    }

    @Test
    fun reconectarRestauraAtivoSemAcaoDoUsuario() {
        shell("cmd notification disallow_listener $COMPONENTE")
        aguardarEstado(EstadoListener.PARADO, MotivoParada.PERMISSAO_REVOGADA)

        shell("cmd notification allow_listener $COMPONENTE")

        val estado = aguardarEstado(EstadoListener.ATIVO)
        assertNull(estado.motivo)
    }

    @Test
    fun notificacaoNaoFinanceiraContaComoIgnoradaEAtualizaUltimaLeitura() {
        val mes = mesAtual()
        val antes = contadores(mes)
        val inicio = Instant.now()

        postar("Seu limite", "pingo-teste-ITAU", TEXTO_RUIDO_ITAU)

        val depois = aguardarContadores(mes) { atuais -> atuais.ignoradas == antes.ignoradas + 1 }
        assertEquals(antes.naoEntendidas, depois.naoEntendidas)
        aguardarLeituraApos(InstituicaoFinanceira.ITAU, inicio)
    }

    @Test
    fun textoForaDosPadroesContaComoNaoEntendida() {
        val mes = mesAtual()
        val antes = contadores(mes)

        postar("Aviso", "pingo-teste-ITAU", TEXTO_DESCONHECIDO)

        val depois = aguardarContadores(mes) { atuais -> atuais.naoEntendidas == antes.naoEntendidas + 1 }
        assertEquals(antes.ignoradas, depois.ignoradas)
    }

    @Test
    fun compraReconhecidaIncrementaReconhecidasECriaLancamento() {
        val mes = mesAtual()
        val antes = contadores(mes)
        val valor = valorUnico()
        val inicio = Instant.now()

        postar("Sua compra foi aprovada", "pingo-teste-ITAU-$valor", textoCompra(valor))

        val depois = aguardarContadores(mes) { atuais -> atuais.reconhecidas == antes.reconhecidas + 1 }
        assertEquals(antes.pendentes, depois.pendentes)
        assertEquals(antes.ignoradas, depois.ignoradas)
        assertEquals(antes.naoEntendidas, depois.naoEntendidas)
        aguardarLancamento(valor, inicio)
    }

    @Test
    fun notificacaoDeCompraViraLancamentoNoRoomEmAte2Segundos() {
        val valor = valorUnico()
        val inicio = System.nanoTime()

        postar("Sua compra foi aprovada", "pingo-teste-ITAU-$valor", textoCompra(valor))

        val lancamento = runBlocking {
            withTimeoutOrNull(TEMPO_ESPERA) {
                captura.lancamentoRepository().observeLancamentos()
                    .first { lista -> lista.any { it.valorCentavos == valor && it.origem == OrigemLancamento.NOTIFICACAO } }
                    .first { it.valorCentavos == valor }
            }
        }
        val decorrido = Duration.ofNanos(System.nanoTime() - inicio)

        assertNotNull("lançamento da notificação não apareceu no Room", lancamento)
        assertTrue("captura notificação→lançamento levou ${decorrido.toMillis()}ms", decorrido <= Duration.ofSeconds(2))
    }

    @Test
    fun capturaSemNenhumInstrumentoCadastradoViraPendenteNuloNoRoomEmAte2Segundos() {
        val agora = Instant.now()
        runBlocking {
            captura.instrumentoRepository().observeInstrumentos().first().forEach { instrumento ->
                captura.instrumentoRepository().excluir(instrumento.publicId, agora)
            }
        }
        val instrumentosAtivos = runBlocking { captura.instrumentoRepository().observeInstrumentos().first() }
        assertTrue("o cenário exige zero instrumentos cadastrados", instrumentosAtivos.isEmpty())

        val valor = valorUnico()
        val inicio = System.nanoTime()

        postar("Compra aprovada", "pingo-teste-NUBANK-$valor", textoCompraNubank(valor))

        val lancamento = runBlocking {
            withTimeoutOrNull(TEMPO_ESPERA) {
                captura.lancamentoRepository().observeLancamentos()
                    .first { lista ->
                        lista.any {
                            it.valorCentavos == valor &&
                                it.origem == OrigemLancamento.NOTIFICACAO &&
                                it.instituicaoOrigem == InstituicaoFinanceira.NUBANK
                        }
                    }
                    .first { it.valorCentavos == valor }
            }
        }
        val decorrido = Duration.ofNanos(System.nanoTime() - inicio)
        val instrumentosNubank = runBlocking {
            captura.instrumentoRepository().ativosDaInstituicao(InstituicaoFinanceira.NUBANK)
        }

        assertNotNull("captura sem instrumento não apareceu no Room", lancamento)
        assertNull(lancamento?.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, lancamento?.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, lancamento?.motivoPendencia)
        assertEquals(InstituicaoFinanceira.NUBANK, lancamento?.instituicaoOrigem)
        assertTrue(instrumentosNubank.isEmpty())
        assertTrue("captura notificação→lançamento levou ${decorrido.toMillis()}ms", decorrido <= Duration.ofSeconds(2))
    }

    @Test
    fun doisLancamentosComInstrumentosEmContasDistintasFormamTransferencia() {
        val agora = Instant.now()
        runBlocking {
            captura.contaRepository().salvar(
                Conta(
                    publicId = CONTA_INTER_ID,
                    instituicao = InstituicaoFinanceira.INTER,
                    apelido = "Inter",
                    tipo = TipoConta.CORRENTE,
                    criadoEm = agora,
                    atualizadoEm = agora,
                ),
            )
            captura.instrumentoRepository().salvar(
                Instrumento(
                    publicId = INSTRUMENTO_INTER_ID,
                    contaPublicId = CONTA_INTER_ID,
                    tipo = TipoInstrumento.DEBITO,
                    apelido = "Débito Inter",
                    criadoEm = agora,
                    atualizadoEm = agora,
                ),
            )
            captura.categoriaRepository().criar(NOME_CATEGORIA_PADRAO, TipoCategoria.RECEITA, agora)
        }

        val valor = valorUnico()
        val inicio = Instant.now()

        postar("Pagamento", "pingo-teste-INTER-$valor", textoPagamentoInter(valor))
        aguardarLancamento(valor, inicio)
        postar("Pix recebido", "pingo-teste-ITAU-$valor", textoPixRecebidoItau(valor))

        val par = runBlocking {
            withTimeout(TEMPO_ESPERA) {
                captura.lancamentoRepository().observeLancamentos()
                    .first { lista ->
                        lista.count {
                            it.valorCentavos == valor &&
                                it.origem == OrigemLancamento.NOTIFICACAO &&
                                it.tipo == TipoLancamento.TRANSFERENCIA
                        } == 2
                    }
                    .filter { it.valorCentavos == valor }
            }
        }

        assertEquals(2, par.size)
        assertEquals(setOf(INSTRUMENTO_INTER_ID, INSTRUMENTO_PUBLIC_ID), par.map { it.instrumentoPublicId }.toSet())
        assertEquals(1, par.mapNotNull { it.transferenciaId }.distinct().size)
        par.forEach { lancamento ->
            assertEquals(TipoLancamento.TRANSFERENCIA, lancamento.tipo)
            assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        }
    }

    private fun semearInstrumentoECategoria() = runBlocking {
        val agora = Instant.now()
        captura.contaRepository().salvar(
            Conta(
                publicId = CONTA_PUBLIC_ID,
                instituicao = InstituicaoFinanceira.ITAU,
                apelido = "Itaú",
                tipo = TipoConta.CORRENTE,
                criadoEm = agora,
                atualizadoEm = agora,
            ),
        )
        captura.instrumentoRepository().salvar(
            Instrumento(
                publicId = INSTRUMENTO_PUBLIC_ID,
                contaPublicId = CONTA_PUBLIC_ID,
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão Itaú",
                ultimosDigitos = ULTIMOS_DIGITOS,
                criadoEm = agora,
                atualizadoEm = agora,
            ),
        )
        captura.categoriaRepository().criar(NOME_CATEGORIA_PADRAO, TipoCategoria.DESPESA, agora)
    }

    private fun aguardarLancamento(valorCentavos: Long, inicio: Instant): Lancamento = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            captura.lancamentoRepository().observeLancamentos()
                .first { lista ->
                    lista.any {
                        it.valorCentavos == valorCentavos &&
                            it.origem == OrigemLancamento.NOTIFICACAO &&
                            it.criadoEm.isAfter(inicio.minusSeconds(2))
                    }
                }
                .first { it.valorCentavos == valorCentavos }
        }
    }

    private fun valorUnico(): Long = 1000L + Math.floorMod(System.nanoTime(), 90000L)

    private fun textoCompraNubank(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Compra de R$ $reais,$centavos APROVADA em DL*99 99 para o cartão com final 5678."
    }

    private fun textoPagamentoInter(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        val quando = LocalDateTime.now().minusMinutes(2).format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))
        return "Pagamento de R$ $reais,$centavos feito à MARIA SILVA em $quando."
    }

    private fun textoPixRecebidoItau(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Pix recebido. Você recebeu R$ $reais,$centavos de MARIA SILVA, CPF 123.456.789-00."
    }

    private fun textoCompra(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Ola! Compra aprovada de R$ $reais,$centavos em CLARO *31987905690 " +
            "no dia 10/11 às 00:55 no cartão final $ULTIMOS_DIGITOS. Boas compras."
    }

    private fun postar(titulo: String, tag: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_de_captura.sh")
        script.writeText("cmd notification post -t \"$titulo\" $tag \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun aguardarNotificacaoProcessada() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            val inicio = Instant.now()
            postar("Sonda do Pingo", "pingo-teste-CAJU", TEXTO_SONDA_CAJU)

            val processou = runBlocking {
                withTimeoutOrNull(INTERVALO_SONDAGEM) {
                    dataSource.ultimaLeituraPorApp.first { leituras ->
                        leituras[InstituicaoFinanceira.CAJU]?.isAfter(inicio.minusSeconds(2)) == true
                    }
                }
            }

            if (processou != null) {
                return
            }
        }
        throw AssertionError("listener não processou a notificação de sondagem")
    }

    private fun contadores(mes: YearMonth): ContadoresDoMes = runBlocking { dataSource.contadoresDoMes(mes).first() }

    private fun aguardarLeituraApos(instituicao: InstituicaoFinanceira, inicio: Instant): Instant = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.ultimaLeituraPorApp.first { leituras ->
                leituras[instituicao]?.isAfter(inicio.minusSeconds(2)) == true
            }[instituicao]!!
        }
    }

    private fun aguardarContadores(mes: YearMonth, condicao: (ContadoresDoMes) -> Boolean): ContadoresDoMes {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            val atuais = contadores(mes)
            if (condicao(atuais)) {
                return atuais
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("contadores do mês $mes não evoluíram como esperado: ${contadores(mes)}")
    }

    private fun aguardarEstado(estado: EstadoListener, motivo: MotivoParada? = null) = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.estadoListener.first { atual -> atual.estado == estado && atual.motivo == motivo }
        }
    }

    private fun aguardarContadoresEstaveis() {
        val mes = mesAtual()
        var anterior = contadores(mes)
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            Thread.sleep(INTERVALO_ESTABILIZACAO)
            val atual = contadores(mes)
            if (atual == anterior) {
                return
            }
            anterior = atual
        }
        throw AssertionError("contadores do mês $mes não estabilizaram: ${contadores(mes)}")
    }

    private fun aguardarAcessoRevogado() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            if (!acesso.concedido()) {
                return
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("acesso às notificações continuou concedido após a revogação")
    }

    private fun aguardarNotificacaoPersistente(): android.service.notification.StatusBarNotification {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            gerenciadorNotificacoes.activeNotifications
                .firstOrNull { notificacao -> notificacao.id == NotificacoesDeCaptura.ID_SERVICO }
                ?.let { return it }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("notificação persistente do foreground service não apareceu")
    }

    private fun mesAtual(): YearMonth = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()))

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().decodeToString()
        }
    }

    private companion object {
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        const val CONTA_PUBLIC_ID = "captura-conta-itau"
        const val INSTRUMENTO_PUBLIC_ID = "captura-instrumento-itau"
        const val CONTA_INTER_ID = "captura-conta-inter"
        const val INSTRUMENTO_INTER_ID = "captura-instrumento-inter"
        const val ULTIMOS_DIGITOS = "1234"
        const val NOME_CATEGORIA_PADRAO = "Outros"
        const val TEMPO_ESPERA = 20_000L
        const val INTERVALO_CONSULTA = 100L
        const val INTERVALO_ESTABILIZACAO = 2_000L
        const val INTERVALO_SONDAGEM = 2_000L
        const val TEXTO_SONDA_CAJU = "Wellhub"
        const val TEXTO_RUIDO_ITAU = "Mais controle para gerenciar seu limite"
        const val TEXTO_DESCONHECIDO = "Seu extrato esta disponivel no aplicativo"
    }
}
