package com.pingo.revisao

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.PermissaoDoPingo
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
import com.pingo.core.notifications.NotificacoesDeRevisao
import com.pingo.core.notifications.RevisarPendenciaReceiver
import com.pingo.core.notifications.di.CapturaStatusEntryPoint
import com.pingo.core.notifications.di.RevisaoNotificacoesEntryPoint
import com.pingo.support.prepararListenerDeCaptura
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class NotificacaoDeRevisaoTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val revisao: RevisaoNotificacoesEntryPoint =
        EntryPointAccessors.fromApplication(contexto, RevisaoNotificacoesEntryPoint::class.java)
    private val capturaStatus: CapturaStatusEntryPoint =
        EntryPointAccessors.fromApplication(contexto, CapturaStatusEntryPoint::class.java)
    private val gerenciador: NotificationManager = contexto.getSystemService(NotificationManager::class.java)

    @Before
    fun preparaAmbiente() {
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        runBlocking { revisao.permissoesDataSource().registrarNotificacoesDoPingo(PermissaoDoPingo.PERMITIDA) }
        limparBanco()
        limparNotificacoesDeRevisao()
    }

    @After
    fun limpaAmbiente() {
        limparBanco()
        limparNotificacoesDeRevisao()
        runBlocking { revisao.permissoesDataSource().registrarNotificacoesDoPingo(PermissaoDoPingo.PERMITIDA) }
    }

    @Test
    fun notificacaoTrazDadosEstruturadosSemTextoBrutoEAcoesDeConfirmarECorrigir() {
        val categoria = semearInstrumentoECategoria()
        salvarPendente(publicId = PRIMEIRA, categoria = categoria)

        val notificacao = aguardarNotificacao(PRIMEIRA)
        val extras = notificacao.notification.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val textoLongo = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        assertEquals("Lançamento para revisar", titulo)
        assertTrue(texto, texto.contains("Padaria do Zé"))
        assertTrue(texto, texto.contains("R$ 42,90"))
        assertTrue(texto, texto.contains("Alimentação"))
        assertFalse(texto, texto.contains(TEXTO_BRUTO_SENSIVEL))
        assertFalse(textoLongo, textoLongo.contains(TEXTO_BRUTO_SENSIVEL))
        assertFalse(textoLongo, textoLongo.contains("cartao final 1234"))

        val acoes = notificacao.notification.actions.map { it.title.toString() }
        assertEquals(listOf("Confirmar", "Corrigir"), acoes)

        val intencao = NotificacoesDeRevisao.intencaoDeCorrecao(contexto, PRIMEIRA)
        assertEquals(Intent.ACTION_VIEW, intencao.action)
        assertEquals("pingo://detalhe/$PRIMEIRA", intencao.data?.toString())
        assertEquals(PRIMEIRA, intencao.getStringExtra(NotificacoesDeRevisao.EXTRA_PUBLIC_ID))
        assertEquals(PACOTE, intencao.component?.packageName)

        assertEquals(1, notificacoesDeRevisao().count { it.id == NotificacoesDeRevisao.idDe(PRIMEIRA) })
    }

    @Test
    fun confirmarPelaAcaoDaNotificacaoResolveSemAbrirOAppERemoveANotificacao() {
        val categoria = semearInstrumentoECategoria()
        salvarPendente(publicId = PRIMEIRA, categoria = categoria)

        val notificacao = aguardarNotificacao(PRIMEIRA)
        acao(notificacao, "Confirmar").actionIntent.send()

        aguardarStatus(PRIMEIRA, StatusLancamento.CONFIRMADO)
        val confirmado = runBlocking { lancamentos().buscar(PRIMEIRA) }
        assertEquals(StatusLancamento.CONFIRMADO, confirmado?.status)
        assertNull(confirmado?.motivoPendencia)
        aguardarSemNotificacao(PRIMEIRA)

        enviarConfirmacaoDireta(PRIMEIRA)
        Thread.sleep(INTERVALO_DE_ESTABILIZACAO)
        assertEquals(StatusLancamento.CONFIRMADO, runBlocking { lancamentos().buscar(PRIMEIRA) }?.status)
        assertTrue(notificacoesDeRevisao().none { it.id == NotificacoesDeRevisao.idDe(PRIMEIRA) })
    }

    @Test
    fun confirmarSemInstrumentoMantemPendenteERepostaOferecendoCorrigir() {
        salvarPendente(
            publicId = PRIMEIRA,
            categoria = null,
            instrumento = null,
            motivo = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO,
        )

        val notificacao = aguardarNotificacao(PRIMEIRA)
        acao(notificacao, "Confirmar").actionIntent.send()

        val corrigida = aguardarTextoDaNotificacao(PRIMEIRA, NotificacoesDeRevisao.AVISO_CORRIGIR)
        assertEquals(listOf("Confirmar", "Corrigir"), corrigida.notification.actions.map { it.title.toString() })

        assertPendenteComMotivo(PRIMEIRA, MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
        assertNull(runBlocking { lancamentos().buscar(PRIMEIRA) }?.instrumentoPublicId)

        enviarConfirmacaoDireta(PRIMEIRA)
        Thread.sleep(INTERVALO_DE_ESTABILIZACAO)
        assertPendenteComMotivo(PRIMEIRA, MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
        assertTrue(notificacoesDeRevisao().any { it.id == NotificacoesDeRevisao.idDe(PRIMEIRA) })
    }

    @Test
    fun resolvidoPelaRevisaoOuPeloDescarteRemoveANotificacao() {
        val categoria = semearInstrumentoECategoria()
        salvarPendente(publicId = PRIMEIRA, categoria = categoria)
        salvarPendente(publicId = SEGUNDA, categoria = categoria, valorCentavos = 1990)

        aguardarNotificacao(PRIMEIRA)
        aguardarNotificacao(SEGUNDA)

        val confirmacao = runBlocking {
            ConfirmarPendencias(lancamentos(), captura.instrumentoRepository(), Clock.systemDefaultZone()).confirmar(listOf(PRIMEIRA))
        }
        assertEquals(1, confirmacao.confirmados)
        aguardarSemNotificacao(PRIMEIRA)

        runBlocking { lancamentos().excluir(SEGUNDA, Instant.now()) }
        aguardarSemNotificacao(SEGUNDA)
    }

    @Test
    fun semPermissaoParaNotificarNaoPostaENaoQuebraACaptura() {
        runBlocking { revisao.permissoesDataSource().registrarNotificacoesDoPingo(PermissaoDoPingo.NEGADA) }
        salvarPendente(
            publicId = PRIMEIRA,
            categoria = null,
            instrumento = null,
            motivo = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO,
        )

        Thread.sleep(INTERVALO_DE_ESTABILIZACAO)

        assertTrue(notificacoesDeRevisao().none { it.id == NotificacoesDeRevisao.idDe(PRIMEIRA) })
        assertPendenteComMotivo(PRIMEIRA, MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
    }

    @Test
    fun umaNotificacaoPorPendenteMesmoComEmissoesRepetidas() {
        val categoria = semearInstrumentoECategoria()
        salvarPendente(publicId = PRIMEIRA, categoria = categoria)
        salvarPendente(publicId = SEGUNDA, categoria = categoria, valorCentavos = 1990)

        aguardarNotificacao(PRIMEIRA)
        aguardarNotificacao(SEGUNDA)
        salvarPendente(publicId = PRIMEIRA, categoria = categoria, observacao = "reemitida")

        Thread.sleep(INTERVALO_DE_ESTABILIZACAO)

        val atual = notificacoesDeRevisao()
        val detalhes = atual.map {
            "${it.id}:${it.notification.channelId}:${it.notification.extras.keySet()}:" +
                "${it.notification.extras.getString(NotificacoesDeRevisao.EXTRA_PUBLIC_ID)}:" +
                "grupo=${it.notification.group}:flags=${it.notification.flags}"
        }
        assertEquals(1, atual.count { it.id == NotificacoesDeRevisao.idDe(PRIMEIRA) })
        assertEquals(1, atual.count { it.id == NotificacoesDeRevisao.idDe(SEGUNDA) })
        assertEquals("notificações ativas: $detalhes", 2, atual.size)
    }

    @Test
    fun notificacaoApareceNaBandejaDoSistema() {
        val categoria = semearInstrumentoECategoria()
        salvarPendente(publicId = PRIMEIRA, categoria = categoria)
        aguardarNotificacao(PRIMEIRA)

        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        shell("cmd statusbar expand-notifications")
        Thread.sleep(INTERVALO_DE_BANDEJA)

        try {
            val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            assertNotNull("não foi possível capturar a bandeja de notificações", captura)
            val arquivo = File(contexto.getExternalFilesDir(null), "notificacao-revisao.png")
            FileOutputStream(arquivo).use { saida -> captura.compress(Bitmap.CompressFormat.PNG, 100, saida) }
            assertTrue("captura da bandeja vazia", arquivo.length() > 0)
        } finally {
            shell("cmd statusbar collapse")
        }
    }

    @Test
    fun capturaPendenteSemInstrumentoGeraNotificacaoDeRevisao() {
        prepararListenerDeCaptura(::shell)
        runBlocking { capturaStatus.capturaController().reconectar() }
        aguardarListenerAtivo()
        limparBanco()
        limparNotificacoesDeRevisao()

        val valor = valorUnico()
        postarNotificacao(textoCompra(valor))

        val pendente = aguardarCapturaPendente(valor)
        assertEquals(StatusLancamento.PENDENTE, pendente.status)
        assertNull(pendente.instrumentoPublicId)

        val notificacao = aguardarNotificacao(pendente.publicId)
        val conteudo = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
            .mapNotNull { notificacao.notification.extras.getCharSequence(it)?.toString() }
            .joinToString("\n")

        assertTrue(conteudo.contains(valorFormatado(valor)))
        assertTrue(conteudo.contains("Categoria sugerida: Sem categoria"))
        pendente.estabelecimento?.takeIf { it.isNotBlank() }?.let { assertTrue(conteudo.contains(it)) }
        assertFalse(conteudo.contains("Compra aprovada"))
        assertFalse(conteudo.contains("Boas compras"))
    }

    private fun aguardarListenerAtivo() = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            capturaStatus.capturaDataSource().estadoListener.first { it.estado == EstadoListener.ATIVO }
        }
    }

    private fun aguardarCapturaPendente(valorCentavos: Long): Lancamento = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            lancamentos().observeLancamentos().first { lista ->
                lista.any {
                    it.valorCentavos == valorCentavos &&
                        it.status == StatusLancamento.PENDENTE &&
                        it.origem == OrigemLancamento.NOTIFICACAO
                }
            }
        }.first { it.valorCentavos == valorCentavos && it.origem == OrigemLancamento.NOTIFICACAO }
    }

    private fun postarNotificacao(texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_revisao.sh")
        script.writeText("cmd notification post -t \"Compra aprovada\" pingo-teste-ITAU \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun valorUnico(): Long = 10_000L + Math.floorMod(System.nanoTime(), 900_000L)

    private fun valorFormatado(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        val inteiro = reais.toString().reversed().chunked(3).joinToString(".").reversed()
        return "R$ $inteiro,$centavos"
    }

    private fun textoCompra(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Ola! Compra aprovada de R$ $reais,$centavos em CLARO *31987905690 " +
            "no dia 10/11 às 00:55 no cartão final $ULTIMOS_DIGITOS. Boas compras."
    }

    private fun assertPendenteComMotivo(publicId: String, motivo: MotivoPendencia) {
        val registro = runBlocking { lancamentos().buscar(publicId) }
        assertEquals(StatusLancamento.PENDENTE, registro?.status)
        assertEquals(motivo.name, registro?.motivoPendencia)
    }

    private fun semearInstrumentoECategoria(): String = runBlocking {
        val agora = Instant.now()
        captura.contaRepository().salvar(
            Conta(
                publicId = CONTA,
                instituicao = InstituicaoFinanceira.ITAU,
                apelido = "Itaú",
                tipo = TipoConta.CORRENTE,
                criadoEm = agora,
                atualizadoEm = agora,
            ),
        )
        captura.instrumentoRepository().salvar(
            Instrumento(
                publicId = INSTRUMENTO,
                contaPublicId = CONTA,
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão Itaú",
                criadoEm = agora,
                atualizadoEm = agora,
            ),
        )
        val criada = captura.categoriaRepository().criar("Alimentação", TipoCategoria.DESPESA, agora)
        (criada as ResultadoCategoria.Sucesso).categoria.publicId
    }

    private fun salvarPendente(
        publicId: String,
        categoria: String?,
        instrumento: String? = INSTRUMENTO,
        motivo: MotivoPendencia = MotivoPendencia.BAIXA_CONFIANCA,
        valorCentavos: Long = 4290,
        observacao: String? = null,
    ) = runBlocking {
        val agora = Instant.now()
        lancamentos().salvar(
            Lancamento(
                publicId = publicId,
                instrumentoPublicId = instrumento,
                categoriaPublicId = categoria,
                valorCentavos = valorCentavos,
                dataHora = agora,
                tipo = TipoLancamento.DESPESA,
                estabelecimento = "Padaria do Zé",
                status = StatusLancamento.PENDENTE,
                origem = OrigemLancamento.NOTIFICACAO,
                instituicaoOrigem = InstituicaoFinanceira.ITAU,
                confianca = 0.72,
                motivoPendencia = motivo.name,
                hashDuplicata = "hash-$publicId",
                observacao = observacao,
                criadoEm = agora,
                atualizadoEm = agora,
                textoBruto = TEXTO_BRUTO_SENSIVEL,
            ),
        )
    }

    private fun lancamentos() = captura.lancamentoRepository()

    private fun notificacoesDeRevisao(): List<StatusBarNotification> = gerenciador.activeNotifications
        .filter { it.notification.channelId == NotificacoesDeRevisao.CANAL }
        .filter { it.notification.extras.getString(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) != null }

    private fun aguardarNotificacao(publicId: String): StatusBarNotification {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            notificacoesDeRevisao().firstOrNull { it.id == NotificacoesDeRevisao.idDe(publicId) }?.let { return it }
            Thread.sleep(INTERVALO_DE_CONSULTA)
        }
        throw AssertionError("notificação de revisão de $publicId não apareceu: ${notificacoesDeRevisao().map { it.id }}")
    }

    private fun aguardarSemNotificacao(publicId: String) {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            if (notificacoesDeRevisao().none { it.id == NotificacoesDeRevisao.idDe(publicId) }) {
                return
            }
            Thread.sleep(INTERVALO_DE_CONSULTA)
        }
        throw AssertionError("notificação de revisão de $publicId não foi removida")
    }

    private fun aguardarTextoDaNotificacao(publicId: String, trecho: String): StatusBarNotification {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            val notificacao = notificacoesDeRevisao().firstOrNull { it.id == NotificacoesDeRevisao.idDe(publicId) }
            val extras = notificacao?.notification?.extras
            val textos = listOf(Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
                .mapNotNull { extras?.getCharSequence(it)?.toString() }

            if (notificacao != null && textos.any { it.contains(trecho) }) {
                return notificacao
            }
            Thread.sleep(INTERVALO_DE_CONSULTA)
        }
        throw AssertionError("notificação de revisão de $publicId não trouxe '$trecho'")
    }

    private fun aguardarStatus(publicId: String, status: StatusLancamento) = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            lancamentos().observeLancamentos().first { lista ->
                lista.firstOrNull { it.publicId == publicId }?.status == status
            }
        }
    }

    private fun acao(notificacao: StatusBarNotification, titulo: String): Notification.Action =
        notificacao.notification.actions.first { it.title.toString() == titulo }

    private fun enviarConfirmacaoDireta(publicId: String) {
        contexto.sendBroadcast(
            Intent(contexto, RevisarPendenciaReceiver::class.java)
                .setAction(NotificacoesDeRevisao.ACAO_CONFIRMAR)
                .putExtra(NotificacoesDeRevisao.EXTRA_PUBLIC_ID, publicId),
        )
    }

    private fun limparBanco() {
        captura.database().clearAllTables()
    }

    private fun limparNotificacoesDeRevisao() {
        val limite = System.currentTimeMillis() + TEMPO_DE_LIMPEZA
        while (System.currentTimeMillis() < limite) {
            notificacoesDeRevisao().forEach { gerenciador.cancel(it.tag, it.id) }
            if (notificacoesDeRevisao().isEmpty()) {
                return
            }
            Thread.sleep(INTERVALO_DE_CONSULTA)
        }
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().decodeToString()
        }
    }

    private companion object {
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        const val ULTIMOS_DIGITOS = "1234"
        const val CONTA = "conta-revisao"
        const val INSTRUMENTO = "instrumento-revisao"
        const val PRIMEIRA = "revisao-primeira"
        const val SEGUNDA = "revisao-segunda"
        const val TEXTO_BRUTO_SENSIVEL = "Compra aprovada R$ 42,90 PADARIA DO ZE no cartao final 1234"
        const val TEMPO_ESPERA = 20_000L
        const val TEMPO_DE_LIMPEZA = 3_000L
        const val INTERVALO_DE_CONSULTA = 100L
        const val INTERVALO_DE_ESTABILIZACAO = 2_000L
        const val INTERVALO_DE_BANDEJA = 1_500L
    }
}
