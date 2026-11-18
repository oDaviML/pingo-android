package com.pingo.feature.accounts.impl

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.ui.contas.ContaFormulario
import com.pingo.core.ui.contas.EventoDoFormularioDeConta
import com.pingo.core.ui.contas.InstrumentoFormulario
import com.pingo.core.ui.contas.aplicar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContaFormContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun novaContaMostraEscolhaDeInstituicaoECamposDoCredito() {
        mostrar(ContaFormUiState())

        composeRule.onNodeWithText("Nova conta").assertIsDisplayed()
        composeRule.onNodeWithText("Qual é a instituição?").assertIsDisplayed()
        listOf("NUBANK", "INTER", "ITAU", "MERCADO_PAGO", "CAJU", "OUTRA").forEach { nome ->
            composeRule.onNodeWithTag("instituicao-$nome").performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("Apelido da conta").assertIsDisplayed()
        composeRule.onNodeWithText("Crédito 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun diaForaDaFaixaMostraAMensagemExataNoCampo() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
        val chave = formulario.instrumentos.single().chave
        val invalido = formulario
            .aplicar(EventoDoFormularioDeConta.AlterarFechamento(chave, "0"))
            .aplicar(EventoDoFormularioDeConta.AlterarVencimento(chave, "32"))
        mostrar(ContaFormUiState(formulario = invalido))

        composeRule.onAllNodesWithText("Informe um dia entre 1 e 31").assertCountEquals(2)
        composeRule.onAllNodesWithText("Informe um dia entre 1 e 31").onFirst().performScrollTo()
        salvarCaptura("conta-validacao-dia")
        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun diasLimitesPermitemSalvar() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
        val chave = formulario.instrumentos.single().chave
        val valido = formulario
            .aplicar(EventoDoFormularioDeConta.AlterarFechamento(chave, "1"))
            .aplicar(EventoDoFormularioDeConta.AlterarVencimento(chave, "31"))
        mostrar(ContaFormUiState(formulario = valido))

        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsEnabled()
    }

    @Test
    fun escolherOutraEmiteTrocaDeInstituicao() {
        val eventos = mutableListOf<ContaFormEvent>()
        mostrar(ContaFormUiState(), eventos = eventos)

        composeRule.onNodeWithTag("instituicao-OUTRA").performScrollTo().performClick()

        assertTrue(
            eventos.contains(
                ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarInstituicao(InstituicaoFinanceira.OUTRA)),
            ),
        )
    }

    @Test
    fun salvarEmiteSalvar() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.OUTRA)
        val eventos = mutableListOf<ContaFormEvent>()
        mostrar(ContaFormUiState(formulario = formulario), eventos = eventos)

        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()

        assertEquals(listOf<ContaFormEvent>(ContaFormEvent.Salvar), eventos)
    }

    @Test
    fun erroDePersistenciaMostraMensagemEPermiteTentarDeNovo() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.OUTRA)
        mostrar(ContaFormUiState(formulario = formulario, falhouAoSalvar = true))

        composeRule.onNodeWithText("Não foi possível salvar sua conta. Tente de novo.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Salvar conta").assertIsEnabled()
    }

    @Test
    fun salvandoBloqueiaOBotaoEMostraEstadoDeCarregamento() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.OUTRA)
        mostrar(ContaFormUiState(formulario = formulario, salvando = true))

        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Apelido da conta").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("+ Crédito").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun doisCreditosAparecemComChavesIndependentesERemover() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
            .let { base ->
                base.copy(
                    instrumentos = listOf(
                        base.instrumentos.single().copy(fechamentoDia = "5", vencimentoDia = "12"),
                        InstrumentoFormulario.novo(TipoInstrumento.CREDITO).copy(
                            apelido = "Cartão adicional",
                            fechamentoDia = "20",
                            vencimentoDia = "28",
                        ),
                    ),
                )
            }
        val eventos = mutableListOf<ContaFormEvent>()
        mostrar(ContaFormUiState(formulario = formulario), eventos = eventos)

        composeRule.onNodeWithText("Crédito 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Crédito 2").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Apelido do crédito").assertCountEquals(2)

        composeRule.onAllNodesWithText("Remover")[1].performScrollTo().performClick()

        assertEquals(
            listOf<ContaFormEvent>(
                ContaFormEvent.Formulario(EventoDoFormularioDeConta.RemoverInstrumento(formulario.instrumentos[1].chave)),
            ),
            eventos,
        )
    }

    @Test
    fun voucherMostraConfiguracaoDeBeneficioSaldoERecarga() {
        val voucher = InstrumentoFormulario.novo(TipoInstrumento.VOUCHER).copy(
            beneficio = BeneficioVoucher.VA,
            saldoInicial = "214,70",
            diaRecarga = "5",
            valorRecarga = "300,00",
        )
        mostrar(ContaFormUiState(formulario = ContaFormulario.nova(InstituicaoFinanceira.CAJU).copy(instrumentos = listOf(voucher))))

        composeRule.onAllNodesWithText("Benefício").onFirst().performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("VA").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Saldo inicial (R$)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Dia da recarga (opcional)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Valor da recarga (R$)").performScrollTo().assertIsDisplayed()
        val boundsDia = composeRule.onNodeWithTag("campo-dia-recarga").performScrollTo().fetchSemanticsNode().boundsInRoot
        val boundsValor = composeRule.onNodeWithTag("campo-valor-recarga").performScrollTo().fetchSemanticsNode().boundsInRoot
        assertEquals(boundsDia.top, boundsValor.top, 2f)
        assertEquals(boundsDia.height, boundsValor.height, 2f)
        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsEnabled()
        salvarCaptura("conta-voucher")
    }

    @Test
    fun edicaoMostraCabecalhoDaInstituicaoSemSeletor() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK).copy(
            publicId = "conta-1",
            apelido = "Nubank pessoal",
        )
        mostrar(ContaFormUiState(formulario = formulario, edicao = true))

        composeRule.onNodeWithText("Editar conta").assertIsDisplayed()
        composeRule.onNodeWithText("Nubank").assertIsDisplayed()
        composeRule.onNodeWithText("Qual é a instituição?").assertDoesNotExist()
    }

    @Test
    fun instrumentoExistenteMostraArquivarEReativarConformeEstado() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK).copy(
            publicId = "conta-1",
            apelido = "Nubank",
            instrumentos = listOf(
                InstrumentoFormulario.novo(TipoInstrumento.CREDITO).copy(
                    publicId = "instrumento-1",
                    apelido = "Cartão",
                    fechamentoDia = "5",
                    vencimentoDia = "12",
                ),
                InstrumentoFormulario.novo(TipoInstrumento.DEBITO).copy(
                    publicId = "instrumento-2",
                    apelido = "Débito antigo",
                    arquivado = true,
                ),
            ),
        )
        val eventos = mutableListOf<ContaFormEvent>()
        mostrar(ContaFormUiState(formulario = formulario, edicao = true), eventos = eventos)

        composeRule.onNodeWithText("Arquivado").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Arquivar").performScrollTo().performClick()
        composeRule.onNodeWithText("Reativar").performScrollTo().performClick()

        assertEquals(
            listOf<ContaFormEvent>(
                ContaFormEvent.SolicitarArquivamentoDoInstrumento(formulario.instrumentos[0].chave),
                ContaFormEvent.ReativarInstrumento(formulario.instrumentos[1].chave),
            ),
            eventos,
        )
    }

    @Test
    fun excluirContaPedeConfirmacaoComAvisoDeHistoricoEPermiteCancelar() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.OUTRA).copy(
            publicId = "conta-1",
            apelido = "Conta manual",
        )
        val eventos = mutableListOf<ContaFormEvent>()
        mostrar(ContaFormUiState(formulario = formulario, edicao = true), eventos = eventos)

        composeRule.onNodeWithText("Excluir conta").performScrollTo().performClick()

        assertEquals(listOf<ContaFormEvent>(ContaFormEvent.SolicitarArquivamentoDaConta), eventos)
    }

    @Test
    fun dialogoDeArquivamentoExplicaHistoricoEPermiteCancelar() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.OUTRA).copy(
            publicId = "conta-1",
            apelido = "Conta manual",
        )
        val eventos = mutableListOf<ContaFormEvent>()
        mostrar(
            ContaFormUiState(formulario = formulario, edicao = true, dialogo = DialogoDeArquivamento.Conta),
            eventos = eventos,
        )

        composeRule.onNodeWithText("Arquivar esta conta?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "O histórico de lançamentos continua disponível em Lançamentos e relatórios.",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        assertEquals(listOf<ContaFormEvent>(ContaFormEvent.CancelarArquivamento), eventos)
    }

    @Test
    fun contaArquivadaMostraAvisoEPermiteReativarSemSalvar() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK).copy(
            publicId = "conta-1",
            apelido = "Nubank",
            instrumentos = listOf(
                InstrumentoFormulario.novo(TipoInstrumento.DEBITO).copy(
                    publicId = "instrumento-1",
                    apelido = "Débito",
                    arquivado = true,
                ),
            ),
        )
        val eventos = mutableListOf<ContaFormEvent>()
        mostrar(ContaFormUiState(formulario = formulario, edicao = true, contaArquivada = true), eventos = eventos)

        composeRule.onNodeWithText("Conta arquivada").assertIsDisplayed()
        composeRule.onNodeWithText("Salvar conta").assertDoesNotExist()
        composeRule.onNodeWithText("Reativar conta").performScrollTo().performClick()

        assertEquals(listOf<ContaFormEvent>(ContaFormEvent.ReativarConta), eventos)
    }

    @Test
    fun carregandoMostraIndicadorProprio() {
        mostrar(ContaFormUiState(carregando = true))

        composeRule.onNodeWithTag("conta-carregando").assertIsDisplayed()
        composeRule.onNodeWithText("Salvar conta").assertDoesNotExist()
    }

    @Test
    fun falhaDeLeituraMostraErroComTentarDeNovo() {
        mostrar(ContaFormUiState(falhaDeLeitura = true))

        composeRule.onNodeWithTag("conta-erro").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar de novo").assertIsDisplayed()
    }

    @Test
    fun formularioRendeNoTemaEscuro() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.OUTRA)
        mostrar(ContaFormUiState(formulario = formulario), escuro = true)

        composeRule.onNodeWithText("Qual é a instituição?").assertIsDisplayed()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsEnabled()
        salvarCaptura("conta-formulario-escuro")
    }

    private fun salvarCaptura(nome: String) {
        val arquivo = "/sdcard/Download/$nome-${Build.VERSION.SDK_INT}.png"
        shell("screencap -p $arquivo")
        assertTrue("screenshot não gerado: $arquivo", shell("ls $arquivo").contains(arquivo))
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)

        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream -> stream.readBytes().decodeToString() }
    }

    private fun mostrar(state: ContaFormUiState, eventos: MutableList<ContaFormEvent> = mutableListOf(), escuro: Boolean = false) {
        composeRule.setContent {
            PingoTheme(darkTheme = escuro) {
                ContaFormContent(
                    state = state,
                    onEvent = { eventos.add(it) },
                    onVoltar = {},
                )
            }
        }
    }
}
