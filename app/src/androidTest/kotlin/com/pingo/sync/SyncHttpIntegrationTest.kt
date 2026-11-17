package com.pingo.sync

import android.app.Activity
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.common.AppDispatchers
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.categoria.CategoriaSeeder
import com.pingo.core.data.detalhe.ResultadoDaTransferencia
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.data.di.DetalheEntryPoint
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.network.HttpPingoApiClient
import com.pingo.core.network.PingoApiConfig
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.CapturaControllerImpl
import com.pingo.core.notifications.ConexaoDoListener
import com.pingo.core.notifications.EscopoDeCaptura
import com.pingo.core.notifications.RebindDeListenerDoSistema
import com.pingo.core.notifications.ServicoDeCapturaDoAndroid
import com.pingo.support.prepararListenerDeCaptura
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SyncHttpIntegrationTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val host: String? = InstrumentationRegistry.getArguments().getString(ARG_HOST)
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val capturaDataSource: CapturaDataSource = CapturaDataSourceImpl(contexto)
    private val acessoNotificacoes = AcessoNotificacoesDoSistema(contexto)
    private val capturaController: CapturaController = CapturaControllerImpl(
        capturaDataSource = capturaDataSource,
        acessoNotificacoes = acessoNotificacoes,
        rebindDeListener = RebindDeListenerDoSistema(contexto),
        servicoDeCaptura = ServicoDeCapturaDoAndroid(contexto),
        conexaoDoListener = ConexaoDoListener(),
        escopoDeCaptura = EscopoDeCaptura(DefaultAppDispatchers),
        clock = Clock.systemDefaultZone(),
    )
    private val dispatchers = object : AppDispatchers {
        override val io = Dispatchers.IO

        override val default = Dispatchers.Default

        override val main = Dispatchers.Main
    }

    @Test
    fun capturaDaSeamSemInstrumentoSincronizaComOrigemSemTextoBrutoEResolucaoPosterior() = runBlocking {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        withTimeout(TEMPO_TOTAL) {
            val token = "auth0|sync-${UUID.randomUUID()}"
            val api = HttpPingoApiClient(PingoApiConfig(host!!), FixtureAuth(token), dispatchers)
            assertTrue("host de teste inacessível em $host", api.healthCheck())

            val fonte = captura.database()
            fonte.clearAllTables()
            assertTrue(
                "o cenário exige zero instrumentos cadastrados",
                captura.instrumentoRepository().observeInstrumentos().first().isEmpty(),
            )

            garantirListenerConectado()

            val valor = valorUnico()
            val texto = textoCompraInter(valor)
            val pendente = capturarPelaSeam(valor, texto)
            val textoBruto = pendente.textoBruto

            assertNotNull(UUID.fromString(pendente.publicId))
            assertNull(pendente.instrumentoPublicId)
            assertEquals(StatusLancamento.PENDENTE, pendente.status)
            assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, pendente.motivoPendencia)
            assertEquals(OrigemLancamento.NOTIFICACAO, pendente.origem)
            assertEquals(InstituicaoFinanceira.INTER, pendente.instituicaoOrigem)
            assertEquals(TipoLancamento.DESPESA, pendente.tipo)
            assertEquals(valor, pendente.valorCentavos)
            assertNull(pendente.transferenciaId)
            assertTrue("texto bruto da captura não foi preservado", textoBruto?.contains(texto) == true)

            val preferencias = FakePreferences()
            val engine = motor(api, fonte, preferencias, token)
            engine.run()

            assertEquals(textoBruto, captura.lancamentoRepository().buscar(pendente.publicId)?.textoBruto)
            assertNull(captura.lancamentoRepository().buscar(pendente.publicId)?.instrumentoPublicId)

            val primeiroPull = api.pullChanges(null)
            val remoto = primeiroPull.lancamentos.singleOrNull { it.publicId == pendente.publicId }
            assertNotNull("captura da seam não chegou ao Postgres", remoto)
            assertEquals(pendente.publicId, remoto?.publicId)
            assertNull(remoto?.instrumentoPublicId)
            assertEquals("Pendente", remoto?.status)
            assertEquals("Notificacao", remoto?.origem)
            assertEquals("Inter", remoto?.instituicaoOrigem)
            assertEquals("INSTRUMENTO_NAO_ENCONTRADO", remoto?.motivoPendencia)
            assertNull(remoto?.transferenciaId)

            engine.run()
            assertEquals(1, api.pullChanges(null).lancamentos.count { it.publicId == pendente.publicId })
            assertEquals(textoBruto, captura.lancamentoRepository().buscar(pendente.publicId)?.textoBruto)

            motor(api, fonte, FakePreferences(), token).run()
            assertEquals(1, api.pullChanges(null).lancamentos.count { it.publicId == pendente.publicId })
            assertEquals(textoBruto, captura.lancamentoRepository().buscar(pendente.publicId)?.textoBruto)

            val restauracao = bancoEmMemoria()
            try {
                motor(api, restauracao, FakePreferences(), token).run()

                val restaurado = restauracao.lancamentoDao().buscar(pendente.publicId)
                assertEquals(pendente.publicId, restaurado?.publicId)
                assertNull(restaurado?.instrumentoPublicId)
                assertNull(restaurado?.textoBruto)
                assertEquals(InstituicaoFinanceira.INTER, restaurado?.instituicaoOrigem)
                assertEquals("INSTRUMENTO_NAO_ENCONTRADO", restaurado?.motivoPendencia)
                assertEquals(StatusLancamento.PENDENTE, restaurado?.status)
            } finally {
                restauracao.close()
            }

            val resolvidoEm = Instant.now().plusSeconds(10)
            captura.contaRepository().criarComInstrumentos(
                listOf(
                    NovaConta(
                        instituicao = InstituicaoFinanceira.INTER,
                        apelido = "Inter",
                        tipo = TipoConta.CORRENTE,
                        instrumentos = listOf(
                            NovoInstrumento(
                                tipo = TipoInstrumento.DEBITO,
                                apelido = "Débito Inter",
                                ultimosDigitos = "9999",
                            ),
                        ),
                    ),
                ),
                resolvidoEm,
            )
            val instrumento = captura.instrumentoRepository()
                .ativosDaInstituicao(InstituicaoFinanceira.INTER)
                .single()
            captura.lancamentoRepository().salvar(
                pendente.copy(
                    instrumentoPublicId = instrumento.publicId,
                    status = StatusLancamento.CONFIRMADO,
                    motivoPendencia = null,
                    atualizadoEm = resolvidoEm,
                ),
            )

            engine.run()

            val resolvidoRemoto = api.pullChanges(null).lancamentos.single { it.publicId == pendente.publicId }
            assertEquals(instrumento.publicId, resolvidoRemoto.instrumentoPublicId)
            assertEquals("Confirmado", resolvidoRemoto.status)
            assertEquals("Inter", resolvidoRemoto.instituicaoOrigem)
            assertNull(resolvidoRemoto.motivoPendencia)

            val localResolvido = captura.lancamentoRepository().buscar(pendente.publicId)
            assertEquals(instrumento.publicId, localResolvido?.instrumentoPublicId)
            assertEquals(StatusLancamento.CONFIRMADO, localResolvido?.status)
            assertEquals(textoBruto, localResolvido?.textoBruto)
        }
    }

    @Test
    fun direcaoSemTransferenciaSincronizaRestauraERoundtripPreservaOsLados() = runBlocking {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        withTimeout(TEMPO_TOTAL) {
            val token = "auth0|direcao-${UUID.randomUUID()}"
            val api = HttpPingoApiClient(PingoApiConfig(host!!), FixtureAuth(token), dispatchers)
            assertTrue("host de teste inacessível em $host", api.healthCheck())

            val fonte = captura.database()
            fonte.clearAllTables()

            captura.contaRepository().criarComInstrumentos(
                listOf(
                    NovaConta(
                        instituicao = InstituicaoFinanceira.NUBANK,
                        apelido = "Nubank",
                        tipo = TipoConta.CORRENTE,
                        instrumentos = listOf(NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito Nubank")),
                    ),
                    NovaConta(
                        instituicao = InstituicaoFinanceira.INTER,
                        apelido = "Inter",
                        tipo = TipoConta.CORRENTE,
                        instrumentos = listOf(NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito Inter")),
                    ),
                ),
                Instant.now(),
            )

            val contas = captura.contaRepository().observeContas().first()
            val nubank = contas.first { it.instituicao == InstituicaoFinanceira.NUBANK }
            val inter = contas.first { it.instituicao == InstituicaoFinanceira.INTER }
            val instrumentoNubank = captura.instrumentoRepository().observePorConta(nubank.publicId).first().single()
            val instrumentoInter = captura.instrumentoRepository().observePorConta(inter.publicId).first().single()
            val agora = Instant.now()
            val saidaId = UUID.randomUUID().toString()
            val entradaId = UUID.randomUUID().toString()
            val textoLocal = "Pix enviado de R$ 50,00"

            captura.lancamentoRepository().salvar(
                Lancamento(
                    publicId = saidaId,
                    instrumentoPublicId = instrumentoNubank.publicId,
                    valorCentavos = 5000,
                    dataHora = agora,
                    tipo = TipoLancamento.DESPESA,
                    status = StatusLancamento.CONFIRMADO,
                    origem = OrigemLancamento.MANUAL,
                    confianca = 1.0,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    textoBruto = textoLocal,
                ),
            )
            captura.lancamentoRepository().salvar(
                Lancamento(
                    publicId = entradaId,
                    instrumentoPublicId = instrumentoInter.publicId,
                    valorCentavos = 5000,
                    dataHora = agora.plusSeconds(60),
                    tipo = TipoLancamento.RECEITA,
                    status = StatusLancamento.CONFIRMADO,
                    origem = OrigemLancamento.MANUAL,
                    confianca = 1.0,
                    criadoEm = agora,
                    atualizadoEm = agora,
                ),
            )

            val detalhe = EntryPointAccessors.fromApplication(contexto, DetalheEntryPoint::class.java)
            val marcada = detalhe.marcarTransferencia().marcar(saidaId, instrumentoInter.publicId)
            assertTrue(marcada is ResultadoDaTransferencia.Marcada)
            assertEquals(TipoLancamento.DESPESA, captura.lancamentoRepository().buscar(saidaId)?.tipoSemTransferencia)
            assertEquals(TipoLancamento.RECEITA, captura.lancamentoRepository().buscar(entradaId)?.tipoSemTransferencia)

            motor(api, fonte, FakePreferences(), token).run()

            val remotos = api.pullChanges(null).lancamentos.filter { it.publicId == saidaId || it.publicId == entradaId }
            assertEquals(2, remotos.size)
            assertEquals("Despesa", remotos.single { it.publicId == saidaId }.tipoSemTransferencia)
            assertEquals("Receita", remotos.single { it.publicId == entradaId }.tipoSemTransferencia)
            assertEquals(textoLocal, captura.lancamentoRepository().buscar(saidaId)?.textoBruto)

            val restauracao = bancoEmMemoria()
            try {
                motor(api, restauracao, FakePreferences(), token).run()

                val saidaRestaurada = restauracao.lancamentoDao().buscar(saidaId)
                val entradaRestaurada = restauracao.lancamentoDao().buscar(entradaId)
                assertEquals(TipoLancamento.TRANSFERENCIA, saidaRestaurada?.tipo)
                assertEquals(TipoLancamento.DESPESA, saidaRestaurada?.tipoSemTransferencia)
                assertEquals(TipoLancamento.TRANSFERENCIA, entradaRestaurada?.tipo)
                assertEquals(TipoLancamento.RECEITA, entradaRestaurada?.tipoSemTransferencia)
                assertEquals(saidaRestaurada?.transferenciaId, entradaRestaurada?.transferenciaId)
                assertNull(saidaRestaurada?.textoBruto)
            } finally {
                restauracao.close()
            }
        }
    }

    @Test
    fun arquivamentoEmCascataCompletoSincronizaRestauraReativaERepeteSemDuplicar() = runBlocking {
        assumeTrue("informe -e $ARG_HOST=<url do Pingo.Api.TestHost>", !host.isNullOrBlank())

        withTimeout(TEMPO_TOTAL) {
            val token = "auth0|cascata-${UUID.randomUUID()}"
            val api = HttpPingoApiClient(PingoApiConfig(host!!), FixtureAuth(token), dispatchers)
            assertTrue("host de teste inacessível em $host", api.healthCheck())

            val fonte = captura.database()
            fonte.clearAllTables()

            captura.contaRepository().criarComInstrumentos(
                listOf(
                    NovaConta(
                        instituicao = InstituicaoFinanceira.NUBANK,
                        apelido = "Nubank pessoal",
                        tipo = TipoConta.CORRENTE,
                        instrumentos = listOf(
                            NovoInstrumento(
                                tipo = TipoInstrumento.CREDITO,
                                apelido = "Crédito principal",
                                ultimosDigitos = "1234",
                                fechamentoDia = 5,
                                vencimentoDia = 12,
                            ),
                            NovoInstrumento(
                                tipo = TipoInstrumento.CREDITO,
                                apelido = "Crédito sem dígitos",
                                fechamentoDia = 28,
                                vencimentoDia = 5,
                            ),
                            NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito antigo"),
                        ),
                    ),
                    NovaConta(
                        instituicao = InstituicaoFinanceira.CAJU,
                        apelido = "Caju benefícios",
                        tipo = TipoConta.BENEFICIO,
                        instrumentos = listOf(
                            NovoInstrumento(
                                tipo = TipoInstrumento.VOUCHER,
                                apelido = "Caju VR",
                                beneficio = BeneficioVoucher.VR,
                                saldoCentavos = 48620,
                                diaRecarga = 5,
                                valorRecargaCentavos = 30000,
                            ),
                        ),
                    ),
                    NovaConta(
                        instituicao = InstituicaoFinanceira.OUTRA,
                        apelido = "Conta manual",
                        tipo = TipoConta.CORRENTE,
                        instrumentos = listOf(NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito manual")),
                    ),
                ),
                Instant.now(),
            )

            val contas = captura.contaRepository().observeContas().first()
            val nubank = contas.single { it.instituicao == InstituicaoFinanceira.NUBANK }
            val caju = contas.single { it.instituicao == InstituicaoFinanceira.CAJU }
            val outra = contas.single { it.instituicao == InstituicaoFinanceira.OUTRA }
            val instrumentosNubank = captura.instrumentoRepository().observePorConta(nubank.publicId).first()
            val credito = instrumentosNubank.single { it.apelido == "Crédito principal" }
            val creditoSemDigitos = instrumentosNubank.single { it.apelido == "Crédito sem dígitos" }
            val debito = instrumentosNubank.single { it.apelido == "Débito antigo" }
            val voucher = captura.instrumentoRepository().observePorConta(caju.publicId).first().single()
            val debitoManual = captura.instrumentoRepository().observePorConta(outra.publicId).first().single()

            captura.instrumentoRepository().arquivar(debito.publicId, Instant.now())
            captura.contaRepository().arquivar(nubank.publicId, Instant.now())

            val engine = motor(api, fonte, FakePreferences(), token)
            engine.run()

            val remotos = api.pullChanges(null)
            val nubankRemota = remotos.contas.single { it.publicId == nubank.publicId }
            val cajuRemota = remotos.contas.single { it.publicId == caju.publicId }
            val outraRemota = remotos.contas.single { it.publicId == outra.publicId }
            assertTrue(nubankRemota.arquivada)
            assertEquals("Nubank pessoal", nubankRemota.apelido)
            assertEquals("Nubank", nubankRemota.instituicao)
            assertEquals("Corrente", nubankRemota.tipo)
            assertEquals("Caju", cajuRemota.instituicao)
            assertEquals("Beneficio", cajuRemota.tipo)
            assertEquals("Outra", outraRemota.instituicao)
            assertFalse(cajuRemota.arquivada)

            val creditoRemoto = remotos.instrumentos.single { it.publicId == credito.publicId }
            val semDigitosRemoto = remotos.instrumentos.single { it.publicId == creditoSemDigitos.publicId }
            val debitoRemoto = remotos.instrumentos.single { it.publicId == debito.publicId }
            val voucherRemoto = remotos.instrumentos.single { it.publicId == voucher.publicId }
            val manualRemoto = remotos.instrumentos.single { it.publicId == debitoManual.publicId }
            assertTrue(creditoRemoto.arquivado)
            assertEquals(true, creditoRemoto.arquivadoPorCascata)
            assertEquals("1234", creditoRemoto.ultimosDigitos)
            assertEquals(5, creditoRemoto.fechamentoDia)
            assertEquals(12, creditoRemoto.vencimentoDia)
            assertEquals("Crédito sem dígitos", semDigitosRemoto.apelido)
            assertNull(semDigitosRemoto.ultimosDigitos)
            assertEquals(28, semDigitosRemoto.fechamentoDia)
            assertEquals(5, semDigitosRemoto.vencimentoDia)
            assertTrue(debitoRemoto.arquivado)
            assertEquals(false, debitoRemoto.arquivadoPorCascata)
            assertEquals("VR", voucherRemoto.beneficio)
            assertEquals(486.20, voucherRemoto.saldo ?: 0.0, 0.001)
            assertEquals(5, voucherRemoto.diaRecarga)
            assertEquals(300.0, voucherRemoto.valorRecarga ?: 0.0, 0.001)
            assertFalse(manualRemoto.arquivado)

            val outroUsuario = HttpPingoApiClient(
                PingoApiConfig(host),
                FixtureAuth("auth0|isolado-${UUID.randomUUID()}"),
                dispatchers,
            )
            val pullDoOutro = outroUsuario.pullChanges(null)
            assertTrue(pullDoOutro.contas.isEmpty())
            assertTrue(pullDoOutro.instrumentos.isEmpty())

            val restauracao = bancoEmMemoria()
            try {
                motor(api, restauracao, FakePreferences(), token).run()
                motor(api, restauracao, FakePreferences(), token).run()

                assertEquals(3, restauracao.contaDao().observeNaoExcluidas().first().size)
                assertEquals(5, restauracao.instrumentoDao().observeNaoExcluidos().first().size)
                val nubankRestaurada = restauracao.contaDao().buscar(nubank.publicId)
                assertTrue(nubankRestaurada?.arquivada == true)
                assertEquals("Nubank pessoal", nubankRestaurada?.apelido)
                assertEquals(InstituicaoFinanceira.NUBANK, nubankRestaurada?.instituicao)
                assertEquals(TipoConta.CORRENTE, nubankRestaurada?.tipo)
                assertEquals(caju.publicId, restauracao.contaDao().buscar(caju.publicId)?.publicId)
                assertEquals(outra.publicId, restauracao.contaDao().buscar(outra.publicId)?.publicId)

                val creditoRestaurado = restauracao.instrumentoDao().buscar(credito.publicId)
                val semDigitosRestaurado = restauracao.instrumentoDao().buscar(creditoSemDigitos.publicId)
                val debitoRestaurado = restauracao.instrumentoDao().buscar(debito.publicId)
                val voucherRestaurado = restauracao.instrumentoDao().buscar(voucher.publicId)
                assertEquals(true, creditoRestaurado?.arquivado)
                assertEquals(true, creditoRestaurado?.arquivadoPorCascata)
                assertEquals("1234", creditoRestaurado?.ultimosDigitos)
                assertEquals(5, creditoRestaurado?.fechamentoDia)
                assertEquals(12, creditoRestaurado?.vencimentoDia)
                assertEquals(null, semDigitosRestaurado?.ultimosDigitos)
                assertEquals(28, semDigitosRestaurado?.fechamentoDia)
                assertEquals(5, semDigitosRestaurado?.vencimentoDia)
                assertEquals(true, debitoRestaurado?.arquivado)
                assertEquals(false, debitoRestaurado?.arquivadoPorCascata)
                assertEquals(BeneficioVoucher.VR, voucherRestaurado?.beneficio)
                assertEquals(48620L, voucherRestaurado?.saldoCentavos)
                assertEquals(5, voucherRestaurado?.diaRecarga)
                assertEquals(30000L, voucherRestaurado?.valorRecargaCentavos)

                val repositorioRestaurado = ContaRepositoryImpl(
                    restauracao.contaDao(),
                    restauracao.instrumentoDao(),
                    RoomTransacaoLocal(restauracao),
                )
                repositorioRestaurado.reativar(nubank.publicId, Instant.now())

                assertEquals(false, restauracao.contaDao().buscar(nubank.publicId)?.arquivada)
                assertEquals(false, restauracao.instrumentoDao().buscar(credito.publicId)?.arquivado)
                assertEquals(false, restauracao.instrumentoDao().buscar(creditoSemDigitos.publicId)?.arquivado)
                assertEquals(true, restauracao.instrumentoDao().buscar(debito.publicId)?.arquivado)
                assertEquals(false, restauracao.instrumentoDao().buscar(debito.publicId)?.arquivadoPorCascata)
            } finally {
                restauracao.close()
            }
        }
    }

    @After
    fun limpaOBancoCompartilhado() {
        runBlocking {
            captura.database().clearAllTables()
            capturaDataSource.clear()
        }
    }

    private suspend fun garantirListenerConectado() {
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        prepararListenerDeCaptura(::shell)

        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite && !acessoNotificacoes.concedido()) {
            delay(INTERVALO_CONSULTA)
        }

        capturaController.reconectar()
    }

    private suspend fun capturarPelaSeam(valorCentavos: Long, texto: String): Lancamento {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA

        while (System.currentTimeMillis() < limite) {
            postar("Compra no débito", "pingo-teste-INTER", texto)

            val capturado = withTimeoutOrNull(INTERVALO_SONDAGEM) {
                captura.lancamentoRepository().observeLancamentos()
                    .first { lancamentos ->
                        lancamentos.any {
                            it.valorCentavos == valorCentavos &&
                                it.origem == OrigemLancamento.NOTIFICACAO &&
                                it.instituicaoOrigem == InstituicaoFinanceira.INTER
                        }
                    }
                    .first { it.valorCentavos == valorCentavos }
            }

            if (capturado != null) {
                return capturado
            }
        }

        throw AssertionError("captura da seam não apareceu no Room")
    }

    private fun valorUnico(): Long = 100_000L + Math.floorMod(System.nanoTime(), 800_000L)

    private fun textoCompraInter(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Olá FULANO, você acaba de comprar no débito no DL*99 99 o valor de R$ $reais,$centavos."
    }

    private fun postar(titulo: String, tag: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_de_sync.sh")
        script.writeText("cmd notification post -t \"$titulo\" $tag \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().decodeToString()
        }
    }

    private fun motor(api: HttpPingoApiClient, banco: PingoDatabase, preferencias: FakePreferences, token: String) = SyncEngine(
        api = api,
        database = banco,
        preferences = preferencias,
        authRepository = FixtureAuth(token),
        deviceActivator = DeviceActivator(api, preferencias),
        categoriaSeeder = CategoriaSeeder(captura.categoriaRepository(), UsuarioAutenticado(FakeTokenStore())),
    )

    private fun bancoEmMemoria(): PingoDatabase = Room.inMemoryDatabaseBuilder(contexto, PingoDatabase::class.java)
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            },
        )
        .build()

    private class FixtureAuth(private val token: String) : AuthRepository {
        override val session: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.LoggedIn)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Failure

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = token
    }

    private class FakeTokenStore : com.pingo.core.auth.TokenStore {
        override suspend fun read(): com.pingo.core.auth.StoredTokens? = null

        override suspend fun write(tokens: com.pingo.core.auth.StoredTokens) = Unit

        override suspend fun clear() = Unit
    }

    private class FakePreferences : UserPreferencesDataSource {
        private val status = MutableStateFlow(SyncStatus.IDLE)
        private var pull: String? = null
        private var push: Instant? = null
        private var last: Instant? = null
        private val dispositivo = UUID.randomUUID().toString()
        private var ativado: String? = null
        private var consentimento = "2026-10"

        override val syncStatus: Flow<SyncStatus> = status

        override suspend fun setSyncStatus(status: SyncStatus) {
            this.status.value = status
        }

        override suspend fun pullCursor(): String? = pull

        override suspend fun setPullCursor(cursor: String?) {
            pull = cursor
        }

        override suspend fun pushCursor(): Instant? = push

        override suspend fun setPushCursor(instant: Instant) {
            push = instant
        }

        override suspend fun lastSyncAt(): Instant? = last

        override suspend fun setLastSyncAt(instant: Instant) {
            last = instant
        }

        override suspend fun deviceId(): String = dispositivo

        override suspend fun activatedDeviceId(): String? = ativado

        override suspend fun setActivatedDeviceId(deviceId: String) {
            ativado = deviceId
        }

        override suspend fun consentVersion(): String? = consentimento

        override suspend fun consentAt(): Instant? = Instant.parse("2026-10-01T00:00:00Z")

        override suspend fun setConsent(version: String, instant: Instant) {
            consentimento = version
        }

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun clear() {
            pull = null
            push = null
            last = null
            ativado = null
        }
    }

    private companion object {
        const val ARG_HOST = "pingo.syncHost"
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        val TEMPO_TOTAL = 240.seconds
        const val TEMPO_ESPERA = 20_000L
        const val INTERVALO_CONSULTA = 100L
        val INTERVALO_SONDAGEM = 2.seconds
    }
}
