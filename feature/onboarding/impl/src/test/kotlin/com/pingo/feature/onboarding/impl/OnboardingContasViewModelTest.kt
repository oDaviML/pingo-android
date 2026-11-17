package com.pingo.feature.onboarding.impl

import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.InstrumentoEditado
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.testing.TestDispatcherRule
import com.pingo.core.ui.contas.ContaFormulario
import com.pingo.core.ui.contas.EventoDoFormularioDeConta
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class OnboardingContasViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val instrumentoRepository = FakeInstrumentoRepository()
    private val contaRepository = FakeContaRepository(instrumentoRepository)
    private val preferences = FakeUserPreferences()
    private val clock: Clock = Clock.fixed(INSTANTE, ZoneOffset.UTC)

    private lateinit var viewModel: OnboardingContasViewModel

    @Before
    fun preparaViewModel() {
        viewModel = criarViewModel()
    }

    private fun criarViewModel(): OnboardingContasViewModel =
        OnboardingContasViewModel(contaRepository, instrumentoRepository, preferences, clock)

    @Test
    fun `comeca sem bancos escolhidos e sem poder continuar`() {
        assertTrue(viewModel.uiState.value.contas.isEmpty())
        assertFalse(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `escolher um banco cria a conta com o tipo e o instrumento padrao`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        val conta = contaDe(InstituicaoFinanceira.NUBANK)
        assertEquals("Nubank", conta.apelido)
        assertEquals(TipoConta.CORRENTE, conta.tipoConta)
        assertEquals(listOf(TipoInstrumento.CREDITO), conta.instrumentos.map { it.tipo })
        assertEquals("Cartão de crédito", conta.instrumentos.single().apelido)
        assertFalse(viewModel.uiState.value.podeContinuar)

        preencherCredito(InstituicaoFinanceira.NUBANK)
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `credito sem fechamento ou vencimento impede continuar`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarFechamento(chaveDe(InstituicaoFinanceira.NUBANK), "5"))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(chaveDe(InstituicaoFinanceira.NUBANK), "32"))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(chaveDe(InstituicaoFinanceira.NUBANK), "31"))
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `voucher exige beneficio e saldo inicial`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.CAJU))

        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(
            InstituicaoFinanceira.CAJU,
            EventoDoFormularioDeConta.AlterarBeneficio(chaveDe(InstituicaoFinanceira.CAJU), BeneficioVoucher.VR),
        )
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.CAJU, EventoDoFormularioDeConta.AlterarSaldoInicial(chaveDe(InstituicaoFinanceira.CAJU), "250,00"))
        assertTrue(viewModel.uiState.value.podeContinuar)

        val instrumento = contaDe(InstituicaoFinanceira.CAJU).instrumentos.single()
        assertEquals(BeneficioVoucher.VR, instrumento.beneficio)
        assertEquals(25000L, instrumento.paraNovoInstrumento().saldoCentavos)
    }

    @Test
    fun `voucher com valor de recarga malformado nao pode continuar`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.CAJU))
        val chave = chaveDe(InstituicaoFinanceira.CAJU)

        alterar(InstituicaoFinanceira.CAJU, EventoDoFormularioDeConta.AlterarBeneficio(chave, BeneficioVoucher.VR))
        alterar(InstituicaoFinanceira.CAJU, EventoDoFormularioDeConta.AlterarSaldoInicial(chave, "250,00"))
        alterar(InstituicaoFinanceira.CAJU, EventoDoFormularioDeConta.AlterarValorRecarga(chave, "1,234"))

        assertFalse(viewModel.uiState.value.podeContinuar)
        assertEquals("1,234", contaDe(InstituicaoFinanceira.CAJU).instrumentos.single().valorRecarga)
    }

    @Test
    fun `desmarcar um banco o remove da lista`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        assertTrue(viewModel.uiState.value.contas.isEmpty())
    }

    @Test
    fun `caju cria conta de beneficio com voucher`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.CAJU))

        val conta = contaDe(InstituicaoFinanceira.CAJU)
        assertEquals(TipoConta.BENEFICIO, conta.tipoConta)
        assertEquals(listOf(TipoInstrumento.VOUCHER), conta.instrumentos.map { it.tipo })
    }

    @Test
    fun `alterar apelido e tipo da conta mexe so no banco escolhido`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.INTER))

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Roxinho"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarTipoConta(TipoConta.BENEFICIO))

        assertEquals("Roxinho", contaDe(InstituicaoFinanceira.NUBANK).apelido)
        assertEquals(TipoConta.BENEFICIO, contaDe(InstituicaoFinanceira.NUBANK).tipoConta)
        assertEquals("Inter", contaDe(InstituicaoFinanceira.INTER).apelido)
        assertEquals(TipoConta.CORRENTE, contaDe(InstituicaoFinanceira.INTER).tipoConta)
    }

    @Test
    fun `adiciona dois instrumentos do mesmo tipo com dados independentes`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.CREDITO))

        val instrumentos = contaDe(InstituicaoFinanceira.NUBANK).instrumentos
        assertEquals(listOf(TipoInstrumento.CREDITO, TipoInstrumento.CREDITO), instrumentos.map { it.tipo })
        assertNotEquals(instrumentos[0].chave, instrumentos[1].chave)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoInstrumento(instrumentos[1].chave, "Cartão dois"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarFechamento(instrumentos[1].chave, "20"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(instrumentos[1].chave, "28"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarFechamento(instrumentos[0].chave, "5"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(instrumentos[0].chave, "12"))

        val atualizados = contaDe(InstituicaoFinanceira.NUBANK).instrumentos
        assertEquals("Cartão de crédito", atualizados[0].apelido)
        assertEquals("5", atualizados[0].fechamentoDia)
        assertEquals("Cartão dois", atualizados[1].apelido)
        assertEquals("20", atualizados[1].fechamentoDia)
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `marcar outro tipo de instrumento adiciona um instrumento com apelido padrao`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.DEBITO))

        val instrumentos = contaDe(InstituicaoFinanceira.NUBANK).instrumentos
        assertEquals(listOf(TipoInstrumento.CREDITO, TipoInstrumento.DEBITO), instrumentos.map { it.tipo })
        assertEquals("Cartão de débito", instrumentos.last().apelido)
    }

    @Test
    fun `remover um instrumento mantem os outros`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.DEBITO))
        val chaveDoCredito = chaveDe(InstituicaoFinanceira.NUBANK, indice = 0)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.RemoverInstrumento(chaveDoCredito))

        assertEquals(listOf(TipoInstrumento.DEBITO), contaDe(InstituicaoFinanceira.NUBANK).instrumentos.map { it.tipo })
    }

    @Test
    fun `nao remove o ultimo instrumento do banco`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.RemoverInstrumento(chaveDe(InstituicaoFinanceira.NUBANK)))

        assertEquals(listOf(TipoInstrumento.CREDITO), contaDe(InstituicaoFinanceira.NUBANK).instrumentos.map { it.tipo })
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `ultimos digitos aceitam so numeros e guardam no maximo quatro`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        alterar(
            InstituicaoFinanceira.NUBANK,
            EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), "12a34x9"),
        )

        assertEquals("1234", contaDe(InstituicaoFinanceira.NUBANK).instrumentos.single().ultimosDigitos)
    }

    @Test
    fun `apelido em branco ou ultimos digitos incompletos impedem continuar`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("   "))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Nubank"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), "12"))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), ""))
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `continuar cria as contas com instrumentos e conclui o passo`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.ITAU))
        alterar(
            InstituicaoFinanceira.NUBANK,
            EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), "1084"),
        )
        preencherCredito(InstituicaoFinanceira.NUBANK)

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        val criadas = contaRepository.criadas.single()
        assertEquals(listOf(InstituicaoFinanceira.NUBANK, InstituicaoFinanceira.ITAU), criadas.map { it.instituicao })
        assertEquals("1084", criadas.first().instrumentos.single().ultimosDigitos)
        assertEquals(listOf(TipoInstrumento.DEBITO), criadas.last().instrumentos.map { it.tipo })
        assertTrue(preferences.configuradas)
        assertTrue(preferences.pendentes.isEmpty())
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `continuar sem bancos nao cria contas`() {
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertTrue(contaRepository.criadas.isEmpty())
        assertFalse(preferences.configuradas)
        assertFalse(viewModel.uiState.value.concluido)
    }

    @Test
    fun `pular conclui o passo sem criar contas`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        viewModel.onEvent(OnboardingContasEvent.Pular)

        assertTrue(contaRepository.criadas.isEmpty())
        assertTrue(preferences.configuradas)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `falha ao criar contas mostra erro e permite tentar de novo`() {
        contaRepository.failure = IllegalStateException("disco cheio")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(SalvamentoUiState.Error, viewModel.uiState.value.salvamento)
        assertFalse(viewModel.uiState.value.concluido)
        assertFalse(preferences.configuradas)

        contaRepository.failure = null
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(SalvamentoUiState.Idle, viewModel.uiState.value.salvamento)
        assertTrue(viewModel.uiState.value.concluido)
        assertEquals(1, contaRepository.criadas.size)
    }

    @Test
    fun `falha ao marcar o passo nao duplica as contas ao tentar de novo`() {
        preferences.failure = IOException("sem espaco")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(SalvamentoUiState.Error, viewModel.uiState.value.salvamento)
        assertFalse(viewModel.uiState.value.concluido)
        val criada = contaRepository.criadas.single().single()
        assertEquals(1, contaRepository.contasSalvas().size)

        preferences.failure = null
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(1, contaRepository.criadas.size)
        assertEquals(1, contaRepository.contasSalvas().size)
        assertTrue(preferences.configuradas)
        assertTrue(viewModel.uiState.value.concluido)
        assertEquals(InstituicaoFinanceira.NUBANK, criada.instituicao)
    }

    @Test
    fun `falha ao concluir preferencia aplica correcoes no retry preservando ids`() {
        preferences.failure = IOException("sem espaco")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        val contaCriada = contaRepository.contasSalvas().single()
        assertEquals(SalvamentoUiState.Error, viewModel.uiState.value.salvamento)
        assertEquals(contaCriada.publicId, contaDe(InstituicaoFinanceira.NUBANK).publicId)

        preferences.failure = null
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Nubank corrigido"))
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        val edicao = contaRepository.edicoes.single()
        assertEquals(contaCriada.publicId, edicao.publicId)
        assertEquals("Nubank corrigido", edicao.apelido)
        assertEquals(1, contaRepository.criadas.size)
        assertEquals(1, contaRepository.contasSalvas().size)
        assertTrue(preferences.configuradas)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `reabrir apos falha de preferencia recupera contas gravadas sem duplicar`() {
        preferences.failure = IOException("sem espaco")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        val criada = contaRepository.contasSalvas().single()
        val instrumentoCriado = instrumentoRepository.porConta(criada.publicId).single()

        preferences.failure = null
        viewModel = criarViewModel()

        val recuperada = contaDe(InstituicaoFinanceira.NUBANK)
        assertEquals(criada.publicId, recuperada.publicId)
        assertEquals(instrumentoCriado.publicId, recuperada.instrumentos.single().publicId)
        assertEquals("5", recuperada.instrumentos.single().fechamentoDia)
        assertTrue(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Nubank corrigido"))
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(1, contaRepository.criadas.size)
        assertEquals(criada.publicId, contaRepository.edicoes.single().publicId)
        assertEquals(criada.publicId, contaRepository.contasSalvas().single().publicId)
        assertTrue(preferences.configuradas)
        assertTrue(preferences.pendentes.isEmpty())
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `recuperacao preserva contas distintas mesmo com campos iguais`() {
        preferences.failure = IOException("sem espaco")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.ITAU))
        listOf(InstituicaoFinanceira.NUBANK, InstituicaoFinanceira.ITAU).forEach { instituicao ->
            alterar(instituicao, EventoDoFormularioDeConta.AlterarApelidoConta("Conta principal"))
        }
        preencherCredito(InstituicaoFinanceira.NUBANK)
        preencherCredito(InstituicaoFinanceira.ITAU)

        viewModel.onEvent(OnboardingContasEvent.Continuar)
        assertEquals(2, contaRepository.contasSalvas().size)

        preferences.failure = null
        viewModel = criarViewModel()

        val recuperadas = viewModel.uiState.value.contas
        assertEquals(2, recuperadas.size)
        assertEquals(2, recuperadas.mapNotNull { it.publicId }.distinct().size)
        assertEquals(setOf("Conta principal"), recuperadas.map { it.apelido }.toSet())
        assertTrue(
            recuperadas.all { conta ->
                conta.instrumentos.size == 1 && conta.instrumentos.single().publicId != null
            },
        )

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(1, contaRepository.criadas.size)
        assertEquals(2, contaRepository.edicoes.size)
        assertTrue(preferences.configuradas)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `remover banco recuperado arquiva a conta antiga em vez de duplicar`() {
        preferences.failure = IOException("sem espaco")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        val criada = contaRepository.contasSalvas().single()
        preferences.failure = null
        viewModel = criarViewModel()

        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.ITAU))
        preencherCredito(InstituicaoFinanceira.ITAU)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(listOf(criada.publicId), contaRepository.arquivadas)
        assertEquals(2, contaRepository.criadas.size)
        assertTrue(preferences.configuradas)
        assertEquals(2, contaRepository.contasSalvas().size)
    }

    @Test
    fun `pular apos falha de preferencia limpa o marcador sem duplicar`() {
        preferences.failure = IOException("sem espaco")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        preferences.failure = null
        viewModel = criarViewModel()
        viewModel.onEvent(OnboardingContasEvent.Pular)

        assertEquals(1, contaRepository.criadas.size)
        assertTrue(preferences.configuradas)
        assertTrue(preferences.pendentes.isEmpty())
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `continuar durante o salvamento nao grava as contas de novo`() {
        contaRepository.gate = CompletableDeferred()
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        viewModel.onEvent(OnboardingContasEvent.Continuar)
        contaRepository.gate?.complete(Unit)

        assertEquals(1, contaRepository.criadas.size)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `editar durante o salvamento pendente nao altera o formulario enviado`() {
        contaRepository.gate = CompletableDeferred()
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertTrue(viewModel.uiState.value.salvamento == SalvamentoUiState.Loading)
        assertFalse(viewModel.uiState.value.formularioEditavel)

        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.INTER))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Alterado durante o salvamento"))

        assertEquals(listOf(InstituicaoFinanceira.NUBANK), viewModel.uiState.value.contas.map { it.instituicao })
        assertEquals("Nubank", contaDe(InstituicaoFinanceira.NUBANK).apelido)

        contaRepository.gate?.complete(Unit)

        assertEquals("Nubank", contaRepository.criadas.single().single().apelido)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `falha ao salvar libera a edicao para o retry`() {
        contaRepository.failure = IllegalStateException("disco cheio")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertTrue(viewModel.uiState.value.formularioEditavel)

        contaRepository.failure = null
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Corrigido"))
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals("Corrigido", contaRepository.criadas.single().single().apelido)
        assertTrue(viewModel.uiState.value.concluido)
    }

    private fun contaDe(instituicao: InstituicaoFinanceira): ContaFormulario =
        viewModel.uiState.value.contas.first { it.instituicao == instituicao }

    private fun chaveDe(instituicao: InstituicaoFinanceira, indice: Int = 0): String = contaDe(instituicao).instrumentos[indice].chave

    private fun alterar(instituicao: InstituicaoFinanceira, evento: EventoDoFormularioDeConta) {
        viewModel.onEvent(OnboardingContasEvent.AlterarFormulario(instituicao, evento))
    }

    private fun preencherCredito(instituicao: InstituicaoFinanceira) {
        alterar(instituicao, EventoDoFormularioDeConta.AlterarFechamento(chaveDe(instituicao), "5"))
        alterar(instituicao, EventoDoFormularioDeConta.AlterarVencimento(chaveDe(instituicao), "12"))
    }

    private class FakeContaRepository(private val instrumentoRepository: FakeInstrumentoRepository) : ContaRepository {
        val criadas = mutableListOf<List<NovaConta>>()
        val edicoes = mutableListOf<ContaEditada>()
        val arquivadas = mutableListOf<String>()
        var failure: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null
        private val contas = mutableMapOf<String, Conta>()

        fun contasSalvas(): List<Conta> = contas.values.toList()

        override fun observeContas(): Flow<List<Conta>> = flowOf(contas.values.toList())

        override suspend fun buscar(publicId: String): Conta? = contas[publicId]

        override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant, publicIds: List<String>): List<CadastroSalvo> {
            gate?.await()
            failure?.let { throw it }
            criadas.add(contas)
            val ids = publicIds.ifEmpty { contas.map { UUID.randomUUID().toString() } }

            return contas.mapIndexed { indice, nova ->
                val conta = Conta(
                    publicId = ids[indice],
                    instituicao = nova.instituicao,
                    apelido = nova.apelido,
                    tipo = nova.tipo,
                    criadoEm = agora,
                    atualizadoEm = agora,
                )
                this.contas[conta.publicId] = conta
                val instrumentos = nova.instrumentos.map { instrumento -> instrumentoDe(instrumento, conta.publicId, agora) }
                instrumentoRepository.adicionar(instrumentos)
                CadastroSalvo(conta = conta, instrumentos = instrumentos)
            }
        }

        override suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo {
            failure?.let { throw it }
            edicoes.add(edicao)
            val atual = contas[edicao.publicId] ?: throw IllegalArgumentException("Conta inexistente")
            val conta = atual.copy(apelido = edicao.apelido, tipo = edicao.tipo, atualizadoEm = agora)
            contas[conta.publicId] = conta
            val instrumentos = edicao.instrumentos.map { instrumento -> instrumentoEditado(instrumento, conta.publicId, agora) }
            instrumentoRepository.substituir(conta.publicId, instrumentos)

            return CadastroSalvo(conta = conta, instrumentos = instrumentos)
        }

        override suspend fun salvar(conta: Conta) {
            contas[conta.publicId] = conta
        }

        private fun instrumentoDe(novo: NovoInstrumento, contaPublicId: String, agora: Instant): Instrumento = Instrumento(
            publicId = UUID.randomUUID().toString(),
            contaPublicId = contaPublicId,
            tipo = novo.tipo,
            apelido = novo.apelido,
            ultimosDigitos = novo.ultimosDigitos,
            fechamentoDia = novo.fechamentoDia,
            vencimentoDia = novo.vencimentoDia,
            beneficio = novo.beneficio,
            saldoCentavos = novo.saldoCentavos,
            diaRecarga = novo.diaRecarga,
            valorRecargaCentavos = novo.valorRecargaCentavos,
            criadoEm = agora,
            atualizadoEm = agora,
        )

        private suspend fun instrumentoEditado(editado: InstrumentoEditado, contaPublicId: String, agora: Instant): Instrumento {
            val existente = editado.publicId?.let { publicId -> instrumentoRepository.buscar(publicId) }

            return Instrumento(
                publicId = existente?.publicId ?: editado.publicId ?: UUID.randomUUID().toString(),
                contaPublicId = contaPublicId,
                tipo = editado.tipo,
                apelido = editado.apelido,
                ultimosDigitos = editado.ultimosDigitos,
                arquivado = existente?.arquivado ?: false,
                arquivadoPorCascata = existente?.arquivadoPorCascata ?: false,
                fechamentoDia = editado.fechamentoDia,
                vencimentoDia = editado.vencimentoDia,
                beneficio = editado.beneficio,
                saldoCentavos = editado.saldoCentavos,
                diaRecarga = editado.diaRecarga,
                valorRecargaCentavos = editado.valorRecargaCentavos,
                limiteCreditoCentavos = existente?.limiteCreditoCentavos,
                criadoEm = existente?.criadoEm ?: agora,
                atualizadoEm = agora,
            )
        }

        override suspend fun salvarTodas(contas: List<Conta>) {
            contas.forEach { salvar(it) }
        }

        override suspend fun excluir(publicId: String, quando: Instant) = Unit

        override suspend fun arquivar(publicId: String, quando: Instant) {
            arquivadas.add(publicId)
            contas[publicId]?.let { contas[publicId] = it.copy(arquivada = true, atualizadoEm = quando) }
        }

        override suspend fun reativar(publicId: String, quando: Instant) {
            contas[publicId]?.let { contas[publicId] = it.copy(arquivada = false, atualizadoEm = quando) }
        }
    }

    private class FakeInstrumentoRepository : InstrumentoRepository {
        private val instrumentos = MutableStateFlow<List<Instrumento>>(emptyList())

        fun adicionar(novos: List<Instrumento>) {
            instrumentos.value = instrumentos.value + novos
        }

        fun substituir(contaPublicId: String, novos: List<Instrumento>) {
            instrumentos.value = instrumentos.value.filterNot { it.contaPublicId == contaPublicId } + novos
        }

        fun porConta(contaPublicId: String): List<Instrumento> = instrumentos.value.filter { it.contaPublicId == contaPublicId }

        override fun observeInstrumentos(): Flow<List<Instrumento>> = instrumentos

        override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
            instrumentos.map { lista -> lista.filter { it.contaPublicId == contaPublicId } }

        override suspend fun buscar(publicId: String): Instrumento? = instrumentos.value.firstOrNull { it.publicId == publicId }

        override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> = emptyList()

        override suspend fun salvar(instrumento: Instrumento) {
            adicionar(listOf(instrumento))
        }

        override suspend fun salvarTodos(instrumentos: List<Instrumento>) {
            adicionar(instrumentos)
        }

        override suspend fun excluir(publicId: String, quando: Instant) = Unit

        override suspend fun arquivar(publicId: String, quando: Instant) = Unit

        override suspend fun reativar(publicId: String, quando: Instant) = Unit
    }

    private class FakeUserPreferences : UserPreferencesDataSource {
        var configuradas = false
        var pendentes: List<String> = emptyList()
        var failure: Throwable? = null

        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dispositivo-teste"

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = configuradas

        override suspend fun setContasConfiguradas(configuradas: Boolean) {
            failure?.let { throw it }
            this.configuradas = configuradas
        }

        override suspend fun contasOnboardingPendentes(): List<String> = pendentes

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) {
            pendentes = publicIds
        }

        override suspend fun clear() = Unit
    }

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-11-10T12:00:00Z")
    }
}
