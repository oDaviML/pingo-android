package com.pingo.feature.accounts.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.fatura.CompletarDatasDoCartaoUseCase
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContasFinanceiroProjectionImpl
import com.pingo.core.data.impl.FontesContasProjection
import com.pingo.core.data.impl.ReconciliadorDeFaturasLocal
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.data.voucher.AjustarSaldoVoucherUseCase
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.feature.accounts.api.AccountsRoute
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class AccountsFinanceiroRoomTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val dbNome = "accounts-financeiro-room-test.db"

    private lateinit var database: PingoDatabase
    private lateinit var projection: ContasFinanceiroProjectionImpl
    private lateinit var completarDatas: CompletarDatasDoCartaoUseCase
    private lateinit var ajustarSaldo: AjustarSaldoVoucherUseCase
    private lateinit var materializarRecargas: MaterializarRecargasVencidasUseCase
    private lateinit var categorias: CategoriaRepositoryImpl

    @Before
    fun preparaBanco() {
        contexto.getDatabasePath(dbNome).delete()
        abrirDependencias()
    }

    @After
    fun limpaBanco() {
        database.close()
        contexto.getDatabasePath(dbNome).delete()
    }

    @Test
    fun completarDatasPreservaIdReconciliaHistoricoEMaterializaVoucherEAjustaSaldo() = runBlocking {
        semearCenarioCompleto()
        val viewModel = criarAccountsViewModel()

        composeRule.setContent {
            PingoTheme {
                AccountsScreen(
                    onSelecionarAba = {},
                    onNovaConta = {},
                    onAbrirConta = {},
                    viewModel = viewModel,
                )
            }
        }

        aguardarTexto("R$ 150,00")
        composeRule.onNodeWithText("R$ 950,00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Datas não configuradas").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Completar datas").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Dia de fechamento").performTextReplacement("15")
        composeRule.onNodeWithContentDescription("Dia de vencimento").performTextReplacement("22")
        composeRule.onNodeWithTag("salvar-datas-cartao").performClick()

        aguardarTexto("R$ 250,00")
        val instCartao = database.instrumentoDao().buscar("inst-cred-inc")!!
        assertEquals(15, instCartao.fechamentoDia)
        assertEquals(22, instCartao.vencimentoDia)

        composeRule.onNodeWithText("Ajustar saldo").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Novo saldo (R$)").performTextReplacement("600,00")
        composeRule.onNodeWithContentDescription("Observação (opcional)").performTextReplacement("Conferido no app")
        composeRule.onNodeWithTag("salvar-ajuste-voucher").performClick()

        aguardarTexto("R$ 600,00")

        database.close()
        abrirDependencias()
        val faturasVm = FaturasViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AccountsRoute.ARG_INSTRUMENTO_ID to "inst-cred-inc")),
            contasFinanceiroProjection = projection,
            categoriaRepository = categorias,
            completarDatasDoCartaoUseCase = completarDatas,
            clock = clock,
        )
        composeRule.waitUntil(ESPERA_MS) { !faturasVm.uiState.value.carregando }
        assertEquals(25_000L, faturasVm.uiState.value.cicloSelecionado?.totalCentavos)
        assertEquals(1, faturasVm.uiState.value.lancamentosDoCicloSelecionado.size)
    }

    private suspend fun semearCenarioCompleto() {
        database.categoriaDao().upsertAll(
            listOf(
                CategoriaEntity("cat-beneficio", "Benefício", TipoCategoria.RECEITA, true, false, agora, agora, null),
                CategoriaEntity("cat-alim", "Alimentação", TipoCategoria.DESPESA, true, false, agora, agora, null),
            ),
        )
        database.contaDao().upsert(
            ContaEntity("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank", TipoConta.CORRENTE, false, agora, agora, null),
        )
        database.contaDao().upsert(
            ContaEntity("conta-caju", InstituicaoFinanceira.CAJU, "Caju", TipoConta.BENEFICIO, false, agora, agora, null),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "inst-cred-inc",
                contaPublicId = "conta-nu",
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão",
                ultimosDigitos = "4321",
                arquivado = false,
                arquivadoPorCascata = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "inst-deb",
                contaPublicId = "conta-nu",
                tipo = TipoInstrumento.DEBITO,
                apelido = "Débito",
                ultimosDigitos = null,
                arquivado = false,
                arquivadoPorCascata = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        val criadoVoucher = Instant.parse("2026-11-01T12:00:00Z")
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "inst-vr",
                contaPublicId = "conta-caju",
                tipo = TipoInstrumento.VOUCHER,
                apelido = "Refeição",
                ultimosDigitos = null,
                arquivado = false,
                arquivadoPorCascata = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = BeneficioVoucher.VR,
                saldoCentavos = 15_000L,
                diaRecarga = 5,
                valorRecargaCentavos = 80_000L,
                limiteCreditoCentavos = null,
                criadoEm = criadoVoucher,
                atualizadoEm = criadoVoucher,
                excluidoEm = null,
            ),
        )
        database.lancamentoDao().upsert(
            LancamentoEntity(
                publicId = "lanc-cred-1",
                instrumentoPublicId = "inst-cred-inc",
                categoriaPublicId = "cat-alim",
                faturaPublicId = null,
                assinaturaPublicId = null,
                compraPaiPublicId = null,
                estornoDePublicId = null,
                transferenciaId = null,
                valorCentavos = 25_000L,
                dataHora = agora.minusSeconds(3600),
                tipo = TipoLancamento.DESPESA,
                tipoSemTransferencia = null,
                estabelecimento = "Restaurante",
                estabelecimentoNormalizado = "RESTAURANTE",
                beneficio = null,
                saldoObservadoCentavos = null,
                status = StatusLancamento.CONFIRMADO,
                origem = OrigemLancamento.NOTIFICACAO,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                confianca = 1.0,
                motivoPendencia = null,
                hashDuplicata = null,
                observacao = null,
                parcelaNumero = 1,
                totalParcelas = 1,
                valorTotalCompraCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
                textoBruto = null,
            ),
        )
        database.lancamentoDao().upsert(
            LancamentoEntity(
                publicId = "lanc-deb-1",
                instrumentoPublicId = "inst-deb",
                categoriaPublicId = "cat-alim",
                faturaPublicId = null,
                assinaturaPublicId = null,
                compraPaiPublicId = null,
                estornoDePublicId = null,
                transferenciaId = null,
                valorCentavos = 15_000L,
                dataHora = agora.minusSeconds(7200),
                tipo = TipoLancamento.DESPESA,
                tipoSemTransferencia = null,
                estabelecimento = "Mercado",
                estabelecimentoNormalizado = "MERCADO",
                beneficio = null,
                saldoObservadoCentavos = null,
                status = StatusLancamento.CONFIRMADO,
                origem = OrigemLancamento.NOTIFICACAO,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                confianca = 1.0,
                motivoPendencia = null,
                hashDuplicata = null,
                observacao = null,
                parcelaNumero = 1,
                totalParcelas = 1,
                valorTotalCompraCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
                textoBruto = null,
            ),
        )
    }

    private fun criarAccountsViewModel(): AccountsViewModel = AccountsViewModel(
        contasFinanceiroProjection = projection,
        completarDatasDoCartaoUseCase = completarDatas,
        ajustarSaldoVoucherUseCase = ajustarSaldo,
        materializarRecargasVencidasUseCase = materializarRecargas,
        clock = clock,
    )

    private fun abrirDependencias() {
        database = Room.databaseBuilder(contexto, PingoDatabase::class.java, dbNome)
            .allowMainThreadQueries()
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                },
            )
            .build()
        val tx = RoomTransacaoLocal(database)
        val reconciliador = ReconciliadorDeFaturasLocal(
            instrumentoDao = database.instrumentoDao(),
            configuracaoCartaoDao = database.configuracaoCartaoDao(),
            faturaDao = database.faturaDao(),
            lancamentoDao = database.lancamentoDao(),
            clock = clock,
        )
        completarDatas = CompletarDatasDoCartaoUseCase(
            transacaoLocal = tx,
            instrumentoDao = database.instrumentoDao(),
            configuracaoCartaoDao = database.configuracaoCartaoDao(),
            reconciliadorDeFaturas = reconciliador,
            clock = clock,
        )
        ajustarSaldo = AjustarSaldoVoucherUseCase(
            transacaoLocal = tx,
            instrumentoDao = database.instrumentoDao(),
            referenciaDao = database.referenciaSaldoVoucherDao(),
            lancamentoDao = database.lancamentoDao(),
            clock = clock,
        )
        materializarRecargas = MaterializarRecargasVencidasUseCase(
            transacaoLocal = tx,
            instrumentoDao = database.instrumentoDao(),
            recargaDao = database.recargaProgramadaVoucherDao(),
            lancamentoDao = database.lancamentoDao(),
            categoriaDao = database.categoriaDao(),
            clock = clock,
        )
        val fontes = FontesContasProjection(
            contaDao = database.contaDao(),
            instrumentoDao = database.instrumentoDao(),
            faturaDao = database.faturaDao(),
            configuracaoCartaoDao = database.configuracaoCartaoDao(),
            referenciaDao = database.referenciaSaldoVoucherDao(),
            recargaDao = database.recargaProgramadaVoucherDao(),
            lancamentoDao = database.lancamentoDao(),
        )
        projection = ContasFinanceiroProjectionImpl(
            fontes = fontes,
            materializarRecargasVencidas = materializarRecargas,
            clock = clock,
        )
        categorias = CategoriaRepositoryImpl(database.categoriaDao())
    }

    private fun aguardarTexto(texto: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val ESPERA_MS = 5000L
    }
}
