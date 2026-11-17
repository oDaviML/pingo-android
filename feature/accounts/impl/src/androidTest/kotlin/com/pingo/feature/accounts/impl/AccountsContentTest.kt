package com.pingo.feature.accounts.impl

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class AccountsContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun estadoVazioMostraMensagemEPermiteAdicionarConta() {
        var adicionou = false
        mostrar(AccountsUiState(carregando = false), onNovaConta = { adicionou = true })

        composeRule.onNodeWithTag("contas-vazio").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhuma conta cadastrada").assertIsDisplayed()
        salvarCaptura("contas-vazio")
        composeRule.onNodeWithText("Adicionar conta").performClick()

        assertTrue(adicionou)
    }

    @Test
    fun estadoCarregandoMostraIndicadorSemDadosFinais() {
        mostrar(AccountsUiState(carregando = true))

        composeRule.onNodeWithTag("contas-carregando").assertIsDisplayed()
        composeRule.onNodeWithText("Carregando contas…").assertIsDisplayed()
        salvarCaptura("contas-carregando")
        composeRule.onNodeWithText("Adicionar conta").assertDoesNotExist()
    }

    @Test
    fun falhaDeLeituraMostraErroEPermiteTentarDeNovo() {
        var tentouDeNovo = false
        mostrar(AccountsUiState(carregando = false, falha = true), onTentarDeNovo = { tentouDeNovo = true })

        composeRule.onNodeWithTag("contas-erro").assertIsDisplayed()
        composeRule.onNodeWithText("Não foi possível carregar suas contas.").assertIsDisplayed()
        salvarCaptura("contas-erro")
        composeRule.onNodeWithText("Tentar de novo").performClick()

        assertTrue(tentouDeNovo)
    }

    @Test
    fun listaAgrupadaMostraInstituicoesContasEInstrumentosEAbreEdicao() {
        var aberta: String? = null
        mostrar(estadoComContas(), onAbrirConta = { aberta = it })

        composeRule.onNodeWithText("Nubank").assertIsDisplayed()
        composeRule.onNodeWithText("Itaú").assertIsDisplayed()
        composeRule.onNodeWithText("Nubank pessoal").assertIsDisplayed()
        composeRule.onNodeWithText("Itaú principal").assertIsDisplayed()
        composeRule.onNodeWithText("Crédito · Cartão ultravioleta · final 4321").assertIsDisplayed()
        composeRule.onNodeWithText("Crédito · Cartão adicional · final 9876").assertIsDisplayed()
        composeRule.onNodeWithText("Débito · Conta corrente").assertIsDisplayed()
        salvarCaptura("contas-preenchida")

        composeRule.onNodeWithTag("conta-conta-nu").performClick()

        assertEquals("conta-nu", aberta)
    }

    @Test
    fun listaAgrupadaRendeNoTemaEscuro() {
        mostrar(estadoComContas(), escuro = true)

        composeRule.onAllNodesWithText("Contas").assertCountEquals(2)
        composeRule.onNodeWithText("Nubank pessoal").assertIsDisplayed()
        composeRule.onNodeWithTag("conta-conta-itau").assertIsDisplayed()
        salvarCaptura("contas-escuro")
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

    private fun mostrar(
        state: AccountsUiState,
        escuro: Boolean = false,
        onNovaConta: () -> Unit = {},
        onAbrirConta: (String) -> Unit = {},
        onTentarDeNovo: () -> Unit = {},
    ) {
        composeRule.setContent {
            PingoTheme(darkTheme = escuro) {
                AccountsContent(
                    state = state,
                    onNovaConta = onNovaConta,
                    onAbrirConta = onAbrirConta,
                    onTentarDeNovo = onTentarDeNovo,
                    onSelecionarAba = { _: AbaInferior -> },
                )
            }
        }
    }

    private fun estadoComContas(): AccountsUiState {
        val contas = listOf(
            conta("conta-nu", InstituicaoFinanceira.NUBANK, "Nubank pessoal"),
            conta("conta-itau", InstituicaoFinanceira.ITAU, "Itaú principal"),
        )
        val instrumentos = listOf(
            instrumento("nu-1", "conta-nu", TipoInstrumento.CREDITO, "Cartão ultravioleta", "4321"),
            instrumento("nu-2", "conta-nu", TipoInstrumento.CREDITO, "Cartão adicional", "9876"),
            instrumento("itau-1", "conta-itau", TipoInstrumento.DEBITO, "Conta corrente", null),
        )

        return AccountsUiState(carregando = false, grupos = agruparContas(contas, instrumentos))
    }

    private fun conta(publicId: String, instituicao: InstituicaoFinanceira, apelido: String): Conta = Conta(
        publicId = publicId,
        instituicao = instituicao,
        apelido = apelido,
        tipo = TipoConta.CORRENTE,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private fun instrumento(
        publicId: String,
        contaPublicId: String,
        tipo: TipoInstrumento,
        apelido: String,
        ultimosDigitos: String?,
    ): Instrumento = Instrumento(
        publicId = publicId,
        contaPublicId = contaPublicId,
        tipo = tipo,
        apelido = apelido,
        ultimosDigitos = ultimosDigitos,
        fechamentoDia = 5.takeIf { tipo == TipoInstrumento.CREDITO },
        vencimentoDia = 12.takeIf { tipo == TipoInstrumento.CREDITO },
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-11-10T12:00:00Z")
    }
}
