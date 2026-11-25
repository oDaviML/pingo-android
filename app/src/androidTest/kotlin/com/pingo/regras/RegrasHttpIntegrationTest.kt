package com.pingo.regras

import android.app.Activity
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.impl.RegraParserRepositoryImpl
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.AlertaDeCaptura
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoAtualizacaoRegras
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.network.ApiConsent
import com.pingo.core.network.ApiDevice
import com.pingo.core.network.ApiProfile
import com.pingo.core.network.HttpPingoApiClient
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.PingoApiConfig
import com.pingo.core.network.rules.ParserRuleResponse
import com.pingo.core.network.rules.ParserRulesResponse
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult
import com.pingo.core.notifications.AtualizadorDeRegras
import com.pingo.core.notifications.RegrasParaCaptura
import com.pingo.core.notifications.ResultadoAtualizacaoRegras
import com.pingo.core.parser.DefaultParserEngine
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.NotificacaoBruta
import com.pingo.core.parser.PadraoRegra
import com.pingo.core.parser.PadroesRegraCodec
import com.pingo.core.parser.RegrasParserSeed
import com.pingo.core.parser.ResultadoParse
import com.pingo.core.parser.TipoLancamentoPadrao
import com.pingo.core.parser.TransacaoExtraida
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.YearMonth
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class RegrasHttpIntegrationTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val host: String? = InstrumentationRegistry.getArguments().getString(ARG_HOST)
    private val dispatchers = object : AppDispatchers {
        override val io = Dispatchers.IO

        override val default = Dispatchers.Default

        override val main = Dispatchers.Main
    }

    @Test
    fun baixaRegrasDoServidorRealGravaAsCincoNoRoomEExpoeAVersao() = runTest(timeout = 120.seconds) {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        val banco = bancoEmMemoria()
        val repositorio = RegraParserRepositoryImpl(banco.regraParserDao())
        val regras = RegrasParaCaptura(repositorio)
        val captura = FakeCapturaDataSource()

        assertEquals(RegrasParserSeed.VERSAO, repositorio.observeVersaoVigente().first())
        assertTrue(repositorio.versoesEmCache().isEmpty())

        val api = HttpPingoApiClient(PingoApiConfig(host!!), FixtureAuth("rules-${System.nanoTime()}"), dispatchers)
        assertTrue("host de teste inacessível em $host", api.healthCheck())

        val resultado = atualizador(api, repositorio, regras, captura).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Atualizado(RegrasParserSeed.VERSAO), resultado)
        assertEquals(5, banco.regraParserDao().observeAtivas().first().size)
        assertEquals(RegrasParserSeed.VERSAO, banco.regraParserDao().versaoAtivaMaxima())
        assertEquals(RegrasParserSeed.VERSAO, repositorio.observeVersaoVigente().first())
        INSTITUICOES.forEach { instituicao -> assertNotNull(regras.versaoDe(instituicao)) }
        assertEquals(3790L, transacaoItau(regras).valorCentavos)
        assertNotNull(captura.estadoDasRegras().atualizadoEm)
    }

    @Test
    fun semRedeMantemOCacheERegistraErroSemQuebrarACaptura() = runTest(timeout = 120.seconds) {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        val banco = bancoEmMemoria()
        val repositorio = RegraParserRepositoryImpl(banco.regraParserDao())
        val regras = RegrasParaCaptura(repositorio)
        val captura = FakeCapturaDataSource()
        val api = HttpPingoApiClient(PingoApiConfig(host!!), FixtureAuth("rules-offline"), dispatchers)

        atualizador(api, repositorio, regras, captura).atualizar()

        val semRede = HttpPingoApiClient(
            PingoApiConfig("http://127.0.0.1:1/", connectTimeoutMillis = 2_000),
            FixtureAuth("rules-offline"),
            dispatchers,
        )

        val resultado = atualizador(semRede, repositorio, regras, captura).atualizar()

        assertTrue(resultado is ResultadoAtualizacaoRegras.Falha)
        assertEquals(5, banco.regraParserDao().observeAtivas().first().size)
        assertEquals(RegrasParserSeed.VERSAO, repositorio.versoesEmCache().values.maxOrNull())
        assertNotNull(captura.estadoDasRegras().motivoFalha)
        assertEquals(3790L, transacaoItau(regras).valorCentavos)
    }

    @Test
    fun versaoNovaSimuladaTrocaOCacheEAplicaOsPadroesNovos() = runTest(timeout = 120.seconds) {
        val banco = bancoEmMemoria()
        val repositorio = RegraParserRepositoryImpl(banco.regraParserDao())
        val regras = RegrasParaCaptura(repositorio)
        val captura = FakeCapturaDataSource()
        val textoNovo = TEXTO_NOVO_DA_V2

        atualizador(FakePingoApiClient { respostaV1() }, repositorio, regras, captura).atualizar()

        assertEquals(RegrasParserSeed.VERSAO, banco.regraParserDao().versaoAtivaMaxima())
        assertTrue(
            DefaultParserEngine().classificar(
                NotificacaoBruta(texto = textoNovo),
                regras.versaoDe(InstituicaoFinanceira.ITAU),
            ) is ResultadoParse.NaoEntendida,
        )

        val resultado = atualizador(FakePingoApiClient { respostaV2() }, repositorio, regras, captura).atualizar()

        val proximaVersao = RegrasParserSeed.VERSAO + 1
        assertEquals(ResultadoAtualizacaoRegras.Atualizado(proximaVersao), resultado)
        assertEquals(proximaVersao, banco.regraParserDao().versaoAtivaMaxima())
        val ativas = banco.regraParserDao().observeAtivas().first()
        assertEquals(5, ativas.size)
        assertEquals(proximaVersao, ativas.single { it.instituicao == InstituicaoFinanceira.ITAU }.versao)
        val transacao = DefaultParserEngine().classificar(
            NotificacaoBruta(texto = textoNovo),
            regras.versaoDe(InstituicaoFinanceira.ITAU),
        ) as ResultadoParse.Transacao
        assertEquals(1234L, transacao.dados.valorCentavos)
    }

    @Test
    fun versaoNovaPublicadaNoServidorRealTrocaOCacheEAplicaOsPadroesNovos() = runTest(timeout = 120.seconds) {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        val banco = bancoEmMemoria()
        val repositorio = RegraParserRepositoryImpl(banco.regraParserDao())
        val regras = RegrasParaCaptura(repositorio)
        val captura = FakeCapturaDataSource()
        val api = HttpPingoApiClient(
            PingoApiConfig(host!!),
            FixtureAuth("rules-publicada-${System.nanoTime()}"),
            dispatchers,
        )
        val itauV1 = RegrasParserSeed.regras.single { it.instituicao == InstituicaoFinanceira.ITAU }

        try {
            publicarNoServidor(itauV2(itauV1))

            assertTrue(
                DefaultParserEngine().classificar(
                    NotificacaoBruta(texto = TEXTO_NOVO_DA_V2),
                    regras.versaoDe(InstituicaoFinanceira.ITAU),
                ) is ResultadoParse.NaoEntendida,
            )

            val resultado = atualizador(api, repositorio, regras, captura).atualizar()

            assertEquals(ResultadoAtualizacaoRegras.Atualizado(RegrasParserSeed.VERSAO + 1), resultado)
            assertEquals(RegrasParserSeed.VERSAO + 1, banco.regraParserDao().versaoAtivaMaxima())
            assertEquals(
                RegrasParserSeed.VERSAO + 1,
                banco.regraParserDao().observeAtivas().first()
                    .single { it.instituicao == InstituicaoFinanceira.ITAU }.versao,
            )
            val transacao = DefaultParserEngine().classificar(
                NotificacaoBruta(texto = TEXTO_NOVO_DA_V2),
                regras.versaoDe(InstituicaoFinanceira.ITAU),
            ) as ResultadoParse.Transacao
            assertEquals(1234L, transacao.dados.valorCentavos)
        } finally {
            publicarNoServidor(itauV1)
        }
    }

    private suspend fun publicarNoServidor(regra: RegraParser) = withContext(Dispatchers.IO) {
        val conexao = URL("${host!!.trimEnd('/')}/test/parser-rules").openConnection() as HttpURLConnection

        try {
            conexao.requestMethod = "POST"
            conexao.doOutput = true
            conexao.setRequestProperty("Content-Type", "application/json")
            conexao.setRequestProperty("Authorization", "Bearer publicacao-${System.nanoTime()}")
            val corpo = """{"instituicao":"${regra.instituicao.name}","versao":${regra.versao},"padroes":${regra.padroes}}"""
            conexao.outputStream.use { saida -> saida.write(corpo.toByteArray()) }
            assertEquals(204, conexao.responseCode)
        } finally {
            conexao.disconnect()
        }
    }

    private fun itauV2(regra: RegraParser): RegraParser = regra.copy(
        versao = RegrasParserSeed.VERSAO + 1,
        padroes = PadroesRegraCodec.codificar(
            PadroesRegraCodec.decodificar(regra.padroes) + PadraoRegra(
                id = "itau-pagamento-fora-do-padrao",
                tipoLancamento = TipoLancamentoPadrao.COMPRA,
                direcao = DirecaoPadrao.DESPESA,
                ancoras = listOf("Pagamento fora do padrão de R$"),
                extracoes = mapOf("valor" to "Pagamento fora do padrão de R$ {valor} em"),
            ),
        ),
    )

    private fun atualizador(
        api: PingoApiClient,
        repositorio: RegraParserRepositoryImpl,
        regras: RegrasParaCaptura,
        captura: CapturaDataSource,
    ) = AtualizadorDeRegras(api, repositorio, regras, captura, java.time.Clock.systemUTC(), dispatchers)

    private suspend fun transacaoItau(regras: RegrasParaCaptura): TransacaoExtraida {
        val resultado = DefaultParserEngine().classificar(
            NotificacaoBruta(texto = TEXTO_COMPRA_ITAU),
            regras.versaoDe(InstituicaoFinanceira.ITAU),
        )

        return (resultado as ResultadoParse.Transacao).dados
    }

    private fun bancoEmMemoria(): PingoDatabase = Room.inMemoryDatabaseBuilder(contexto, PingoDatabase::class.java)
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            },
        )
        .build()

    private fun respostaV1() = ParserRulesResponse(
        versao = RegrasParserSeed.VERSAO,
        regras = RegrasParserSeed.regras.map {
            it.paraResposta()
        },
    )

    private fun respostaV2(): ParserRulesResponse {
        val novaVersao = RegrasParserSeed.VERSAO + 1
        val regras = RegrasParserSeed.regras.map { regra ->
            if (regra.instituicao != InstituicaoFinanceira.ITAU) {
                regra
            } else {
                regra.copy(
                    publicId = "servidor-itau-v$novaVersao",
                    versao = novaVersao,
                    padroes = PadroesRegraCodec.codificar(
                        PadroesRegraCodec.decodificar(regra.padroes) + PadraoRegra(
                            id = "itau-pagamento-fora-do-padrao",
                            tipoLancamento = TipoLancamentoPadrao.COMPRA,
                            direcao = DirecaoPadrao.DESPESA,
                            ancoras = listOf("Pagamento fora do padrão de R$"),
                            extracoes = mapOf("valor" to "Pagamento fora do padrão de R$ {valor} em"),
                        ),
                    ),
                )
            }
        }

        return ParserRulesResponse(versao = novaVersao, regras = regras.map { it.paraResposta() })
    }

    private fun RegraParser.paraResposta() = ParserRuleResponse(
        publicId = publicId,
        instituicao = instituicao.name,
        versao = versao,
        publicadaEm = "2026-11-10T00:00:00Z",
        padroes = Json.parseToJsonElement(padroes).jsonArray,
    )

    private companion object {
        const val ARG_HOST = "pingo.syncHost"
        const val TEXTO_COMPRA_ITAU =
            "Olá! Compra aprovada de R$ 37,90 em CLARO *31987905690 no dia 10/11 às 00:55 no seu cartão Itau. Boas compras."
        const val TEXTO_NOVO_DA_V2 = "Pagamento fora do padrão de R$ 12,34 em LOJA NOVA"

        val INSTITUICOES = setOf(
            InstituicaoFinanceira.NUBANK,
            InstituicaoFinanceira.INTER,
            InstituicaoFinanceira.ITAU,
            InstituicaoFinanceira.MERCADO_PAGO,
            InstituicaoFinanceira.CAJU,
        )
    }
}

private class FixtureAuth(private val token: String) : AuthRepository {
    override val session: kotlinx.coroutines.flow.StateFlow<SessionState> =
        MutableStateFlow<SessionState>(SessionState.LoggedIn)

    override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Failure

    override suspend fun logout() = Unit

    override suspend fun accessToken(forceRefresh: Boolean): String? = token
}

private class FakeCapturaDataSource : CapturaDataSource {
    private val regras = MutableStateFlow(EstadoAtualizacaoRegras())

    override val estadoListener: Flow<EstadoListenerPersistido> =
        flowOf(EstadoListenerPersistido(EstadoListener.PARADO))

    override val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>> = flowOf(emptyMap())

    override val observandoDesde: Flow<Instant?> = flowOf(null)

    override val estadoAtualizacaoRegras: Flow<EstadoAtualizacaoRegras> = regras.asStateFlow()

    override val alertaDeCaptura: Flow<AlertaDeCaptura?> = flowOf(null)

    fun estadoDasRegras(): EstadoAtualizacaoRegras = regras.value

    override fun contadoresDoMes(mes: YearMonth): Flow<ContadoresDoMes> = flowOf(ContadoresDoMes(mes))

    override suspend fun registrarAtivo(em: Instant) = Unit

    override suspend fun registrarParado(motivo: MotivoParada, em: Instant) = Unit

    override suspend fun registrarLeitura(instituicao: InstituicaoFinanceira, em: Instant) = Unit

    override suspend fun incrementarContador(mes: YearMonth, contador: ContadorCaptura) = Unit

    override suspend fun registrarRegrasAtualizadas(em: Instant) {
        regras.value = EstadoAtualizacaoRegras(atualizadoEm = em)
    }

    override suspend fun registrarFalhaAoAtualizarRegras(motivo: String, em: Instant) {
        regras.value = regras.value.copy(falhaEm = em, motivoFalha = motivo)
    }

    override suspend fun registrarAlertaDeCaptura(referencia: Instant, em: Instant) = Unit

    override suspend fun clear() = Unit
}

private class FakePingoApiClient(private val regras: () -> ParserRulesResponse) : PingoApiClient {
    override suspend fun healthCheck(): Boolean = true

    override suspend fun getProfile(): ApiProfile = error("não utilizado")

    override suspend fun pushChanges(payload: PushChangesPayload): PushChangesResult = error("não utilizado")

    override suspend fun pullChanges(cursor: String?): PullChangesResult = error("não utilizado")

    override suspend fun getParserRules(): ParserRulesResponse = regras()

    override suspend fun activateDevice(deviceId: String, deviceName: String?): ApiDevice = error("não utilizado")

    override suspend fun recordConsent(version: String): ApiConsent = error("não utilizado")

    override suspend fun deleteAccount() = Unit
}
