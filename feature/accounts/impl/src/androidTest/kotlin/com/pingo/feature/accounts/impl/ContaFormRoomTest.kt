package com.pingo.feature.accounts.impl

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.database.PingoDatabase
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.ui.contas.EventoDoFormularioDeConta
import com.pingo.feature.accounts.api.AccountsRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class ContaFormRoomTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)

    private lateinit var database: PingoDatabase
    private lateinit var contas: ContaRepositoryImpl
    private lateinit var instrumentos: InstrumentoRepositoryImpl
    private lateinit var lancamentos: LancamentoRepositoryImpl

    @Before
    fun preparaBanco() {
        database = Room.inMemoryDatabaseBuilder(contexto, PingoDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                },
            )
            .build()
        contas = ContaRepositoryImpl(database.contaDao(), database.instrumentoDao(), RoomTransacaoLocal(database))
        instrumentos = InstrumentoRepositoryImpl(database.instrumentoDao())
        lancamentos = LancamentoRepositoryImpl(database.lancamentoDao())
    }

    @After
    fun fechaBanco() {
        database.close()
    }

    @Test
    fun criarContaComDoisCreditosEVoucherPersisteIdsEDadosIndependentes() = runBlocking {
        val viewModel = criarViewModel()
        val primeiroCredito = viewModel.uiState.value.formulario.instrumentos.single().chave
        alterar(viewModel, EventoDoFormularioDeConta.AlterarFechamento(primeiroCredito, "5"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarVencimento(primeiroCredito, "12"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarUltimosDigitos(primeiroCredito, "1111"))
        alterar(viewModel, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.CREDITO))
        val segundoCredito = viewModel.uiState.value.formulario.instrumentos[1].chave
        alterar(viewModel, EventoDoFormularioDeConta.AlterarApelidoInstrumento(segundoCredito, "Cartão adicional"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarFechamento(segundoCredito, "20"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarVencimento(segundoCredito, "28"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarUltimosDigitos(segundoCredito, "2222"))
        alterar(viewModel, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.VOUCHER))
        val voucher = viewModel.uiState.value.formulario.instrumentos[2].chave
        alterar(viewModel, EventoDoFormularioDeConta.AlterarBeneficio(voucher, BeneficioVoucher.VR))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarSaldoInicial(voucher, "486,20"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarDiaRecarga(voucher, "5"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarValorRecarga(voucher, "300,00"))

        viewModel.onEvent(ContaFormEvent.Salvar)
        aguardarConclusao(viewModel)

        val conta = contas.observeContas().first().single()
        val persistidos = instrumentos.observePorConta(conta.publicId).first()
        assertEquals(3, persistidos.size)
        val creditos = persistidos.filter { it.tipo == TipoInstrumento.CREDITO }
        assertEquals(2, creditos.size)
        assertNotEquals(creditos[0].publicId, creditos[1].publicId)
        assertEquals(setOf(5, 20), creditos.mapNotNull { it.fechamentoDia }.toSet())
        assertEquals(setOf(12, 28), creditos.mapNotNull { it.vencimentoDia }.toSet())
        assertEquals(setOf("1111", "2222"), creditos.mapNotNull { it.ultimosDigitos }.toSet())
        val voucherPersistido = persistidos.single { it.tipo == TipoInstrumento.VOUCHER }
        assertEquals(BeneficioVoucher.VR, voucherPersistido.beneficio)
        assertEquals(48620L, voucherPersistido.saldoCentavos)
        assertEquals(5, voucherPersistido.diaRecarga)
        assertEquals(30000L, voucherPersistido.valorRecargaCentavos)
    }

    @Test
    fun editarPreservaIdsDosInstrumentosEVinculoDoLancamento() = runBlocking {
        val conta = semearContaComVoucher()
        val instrumentosOriginais = instrumentos.observePorConta(conta.publicId).first()
        val voucher = instrumentosOriginais.single { it.tipo == TipoInstrumento.VOUCHER }
        val lancamento = semearLancamento(voucher.publicId)

        val viewModel = criarViewModel(conta.publicId)
        assertTrue(viewModel.uiState.value.podeSalvar)
        alterar(viewModel, EventoDoFormularioDeConta.AlterarApelidoConta("Caju pessoal"))
        alterar(viewModel, EventoDoFormularioDeConta.AlterarSaldoInicial(voucher.publicId, "500,00"))
        alterar(viewModel, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.DEBITO))
        viewModel.onEvent(ContaFormEvent.Salvar)
        aguardarConclusao(viewModel)

        val contaAtualizada = contas.buscar(conta.publicId)!!
        assertEquals(conta.publicId, contaAtualizada.publicId)
        assertEquals("Caju pessoal", contaAtualizada.apelido)
        val instrumentosAtualizados = instrumentos.observePorConta(conta.publicId).first()
        assertEquals(2, instrumentosAtualizados.size)
        assertEquals(
            instrumentosOriginais.map { it.publicId }.toSet(),
            instrumentosAtualizados.filter { it.tipo != TipoInstrumento.DEBITO }.map { it.publicId }.toSet(),
        )
        assertEquals(50000L, instrumentosAtualizados.single { it.publicId == voucher.publicId }.saldoCentavos)

        val lancamentoAtualizado = lancamentos.buscar(lancamento.publicId)!!
        assertEquals(lancamento.instrumentoPublicId, lancamentoAtualizado.instrumentoPublicId)
        assertEquals(lancamento.valorCentavos, lancamentoAtualizado.valorCentavos)
        assertEquals(lancamento.estabelecimento, lancamentoAtualizado.estabelecimento)
    }

    @Test
    fun reabrirEdicaoApresentaOsMesmosCamposPersistidos() = runBlocking {
        val conta = semearContaComVoucher()
        val viewModel = criarViewModel(conta.publicId)

        val formulario = viewModel.uiState.value.formulario
        val voucher = formulario.instrumentos.single { it.tipo == TipoInstrumento.VOUCHER }
        assertEquals(conta.publicId, formulario.publicId)
        assertEquals("Caju", formulario.apelido)
        assertEquals(BeneficioVoucher.VA, voucher.beneficio)
        assertEquals("214,70", voucher.saldoInicial)
        assertEquals("5", voucher.diaRecarga)
        assertEquals("300,00", voucher.valorRecarga)
    }

    @Test
    fun falhaAoPersistirNaoDeixaContaParcialERetryConcluiUmaVez() = runBlocking {
        val viewModel = criarViewModel()

        viewModel.onEvent(ContaFormEvent.Salvar)

        assertTrue(contas.observeContas().first().isEmpty())
        assertTrue(instrumentos.observeInstrumentos().first().isEmpty())

        alterar(viewModel, EventoDoFormularioDeConta.AlterarInstituicao(InstituicaoFinanceira.OUTRA))
        viewModel.onEvent(ContaFormEvent.Salvar)
        aguardarConclusao(viewModel)

        assertEquals(1, contas.observeContas().first().size)
        assertEquals(1, instrumentos.observeInstrumentos().first().size)
        assertEquals(InstituicaoFinanceira.OUTRA, contas.observeContas().first().single().instituicao)
    }

    private fun criarViewModel(contaPublicId: String? = null): ContaFormViewModel {
        val argumentos = if (contaPublicId == null) emptyMap() else mapOf(AccountsRoute.ARG_CONTA_ID to contaPublicId)
        val viewModel = ContaFormViewModel(contas, instrumentos, clock, SavedStateHandle(argumentos))

        if (contaPublicId != null) {
            runBlocking {
                withTimeout(TEMPO_DE_ESPERA) { viewModel.uiState.first { !it.carregando } }
            }
        }

        return viewModel
    }

    private fun alterar(viewModel: ContaFormViewModel, evento: EventoDoFormularioDeConta) {
        viewModel.onEvent(ContaFormEvent.Formulario(evento))
    }

    private suspend fun aguardarConclusao(viewModel: ContaFormViewModel) {
        withTimeout(TEMPO_DE_ESPERA) {
            viewModel.uiState.first { it.concluido }
        }
    }

    private suspend fun semearContaComVoucher(): com.pingo.core.model.Conta {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.VOUCHER,
                            apelido = "Caju VA",
                            beneficio = BeneficioVoucher.VA,
                            saldoCentavos = 21470L,
                            diaRecarga = 5,
                            valorRecargaCentavos = 30000L,
                        ),
                    ),
                ),
            ),
            agora,
        )

        return contas.observeContas().first().single()
    }

    private suspend fun semearLancamento(instrumentoPublicId: String): Lancamento {
        val lancamento = Lancamento(
            publicId = "lancamento-1",
            instrumentoPublicId = instrumentoPublicId,
            valorCentavos = 1500L,
            dataHora = agora,
            tipo = TipoLancamento.DESPESA,
            estabelecimento = "Padaria",
            status = StatusLancamento.CONFIRMADO,
            origem = OrigemLancamento.MANUAL,
            confianca = 1.0,
            criadoEm = agora,
            atualizadoEm = agora,
        )
        lancamentos.salvar(lancamento)

        return lancamento
    }

    private companion object {
        const val TEMPO_DE_ESPERA = 5_000L
    }
}
