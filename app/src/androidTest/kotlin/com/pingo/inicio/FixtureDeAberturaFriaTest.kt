package com.pingo.inicio

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.auth.KeystoreTokenStore
import com.pingo.core.auth.StoredTokens
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FixtureDeAberturaFriaTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val tokens = KeystoreTokenStore(contexto, DefaultAppDispatchers)
    private val preferencias = UserPreferencesDataSourceImpl(contexto)

    @Test
    fun preparaSessaoConsentimentoEContasComVinteLancamentos(): Unit = runBlocking {
        assumeTrue(medicaoDeAberturaHabilitada)
        captura.database().clearAllTables()
        val agora = Instant.now()
        preferencias.setConsent(VERSAO_DO_CONSENTIMENTO, agora)
        preferencias.setContasConfiguradas(true)
        preferencias.setActivatedDeviceId(preferencias.deviceId())
        tokens.write(
            StoredTokens(
                accessToken = "fixture-access",
                refreshToken = "fixture-refresh",
                expiresAtEpochMillis = agora.toEpochMilli() + TimeUnit.DAYS.toMillis(30),
            ),
        )
        captura.contaRepository().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.ITAU,
                    apelido = "Itaú",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.CREDITO,
                            apelido = "Cartão Itaú",
                            ultimosDigitos = "1234",
                            fechamentoDia = 5,
                            vencimentoDia = 12,
                        ),
                    ),
                ),
            ),
            agora,
        )
        val instrumento = captura.instrumentoRepository().observeInstrumentos().first().single()
        val categoria = (
            captura.categoriaRepository().criar(
                "Outros",
                TipoCategoria.DESPESA,
                agora,
            ) as ResultadoCategoria.Sucesso
            ).categoria
        repeat(LANCAMENTOS) { indice ->
            captura.lancamentoRepository().salvar(
                Lancamento(
                    publicId = "abertura-fixture-$indice",
                    instrumentoPublicId = instrumento.publicId,
                    categoriaPublicId = categoria.publicId,
                    valorCentavos = 1000L + indice,
                    dataHora = agora.minusSeconds(indice * 900L),
                    tipo = TipoLancamento.DESPESA,
                    estabelecimento = "Loja $indice",
                    status = StatusLancamento.CONFIRMADO,
                    origem = OrigemLancamento.MANUAL,
                    instituicaoOrigem = InstituicaoFinanceira.ITAU,
                    confianca = 0.9,
                    criadoEm = agora,
                    atualizadoEm = agora,
                ),
            )
        }
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        shell("cmd notification allow_listener $PACOTE/$SERVICO_DE_ESCUTA")
    }

    @Test
    fun removeSessaoConsentimentoEContas(): Unit = runBlocking {
        assumeTrue(medicaoDeAberturaHabilitada)
        captura.database().clearAllTables()
        tokens.clear()
        preferencias.clear()
        shell("cmd notification disallow_listener $PACOTE/$SERVICO_DE_ESCUTA")
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream -> stream.readBytes().decodeToString() }
    }

    private val medicaoDeAberturaHabilitada: Boolean =
        InstrumentationRegistry.getArguments().getString(CHAVE_DE_MEDICAO) == "true"

    private companion object {
        const val CHAVE_DE_MEDICAO = "pingo.medicaoAbertura"
        const val PACOTE = "com.pingo"
        const val SERVICO_DE_ESCUTA = "com.pingo.core.notifications.PingoNotificationListenerService"
        const val VERSAO_DO_CONSENTIMENTO = "v1"
        const val LANCAMENTOS = 20
    }
}
