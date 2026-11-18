package com.pingo.feature.accounts.impl

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.organizacao.ContaComIndicadores
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.model.voucher.UltimaRecargaVoucher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class AccountsFinanceiroContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun subcardsExibemFaturaAbertaCartaoIncompletoDebitoEVoucherComAcoes() {
        var faturaAbertaId: String? = null
        val eventos = mutableListOf<AccountsEvent>()
        val state = criarEstadoFinanceiro()

        mostrar(
            state = state,
            onAbrirFaturas = { faturaAbertaId = it },
            onEvent = { eventos.add(it) },
        )

        composeRule.onNodeWithText("R$ 1.284,50").assertIsDisplayed()
        composeRule.onNodeWithText("Vence em 19 dias").assertIsDisplayed()
        composeRule.onNodeWithTag("instrumento-credito-cred-ok").performClick()
        assertEquals("cred-ok", faturaAbertaId)

        composeRule.onNodeWithTag("instrumento-credito-cred-inc").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Datas não configuradas").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 0,00").assertDoesNotExist()
        composeRule.onNodeWithText("Completar datas").performClick()
        assertTrue(eventos.any { it is AccountsEvent.AbrirCompletarDatas })

        composeRule.onNodeWithTag("instrumento-debito-deb-1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Gasto do mês").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 1.020,00").assertIsDisplayed()

        composeRule.onNodeWithTag("instrumento-voucher-vou-1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("R$ 486,20").assertIsDisplayed()
        composeRule.onNodeWithText("Ajustar saldo").performScrollTo().performClick()
        assertTrue(eventos.any { it is AccountsEvent.AbrirAjustarSaldo })
    }

    @Test
    fun modaisDeCompletarDatasEAjustarSaldoRespeitamValidacoesETemaEscuro() {
        var state by mutableStateOf(
            criarEstadoFinanceiro().copy(
                modalCompletarDatas = ModalCompletarDatasUiState(
                    instrumentoPublicId = "cred-inc",
                    tituloCartao = "Crédito · Cartão sem datas",
                    fechamentoDia = "0",
                    vencimentoDia = "35",
                ),
            ),
        )
        composeRule.setContent {
            PingoTheme(darkTheme = true) {
                AccountsContent(
                    state = state,
                    onNovaConta = {},
                    onAbrirConta = {},
                    onTentarDeNovo = {},
                    onSelecionarAba = { _: AbaInferior -> },
                    onAbrirFaturas = {},
                    onEvent = {},
                )
            }
        }
        composeRule.onNodeWithTag("modal-completar-datas").assertIsDisplayed()
        composeRule.onNodeWithTag("salvar-datas-cartao").assertIsNotEnabled()

        state = criarEstadoFinanceiro().copy(
            modalAjustarSaldo = ModalAjustarSaldoUiState(
                instrumentoPublicId = "vou-1",
                tituloVoucher = "VR · Refeição",
                saldoAtualCentavos = 48_620L,
                modo = ModoAjusteVoucher.DELTA,
                valorTexto = "12,345",
            ),
        )
        composeRule.onNodeWithTag("modal-ajustar-saldo").assertIsDisplayed()
        composeRule.onNodeWithTag("salvar-ajuste-voucher").assertIsNotEnabled()
    }

    private fun mostrar(
        state: AccountsUiState,
        escuro: Boolean = false,
        onAbrirFaturas: (String) -> Unit = {},
        onEvent: (AccountsEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            PingoTheme(darkTheme = escuro) {
                AccountsContent(
                    state = state,
                    onNovaConta = {},
                    onAbrirConta = {},
                    onTentarDeNovo = {},
                    onSelecionarAba = { _: AbaInferior -> },
                    onAbrirFaturas = onAbrirFaturas,
                    onEvent = onEvent,
                )
            }
        }
    }

    private fun criarEstadoFinanceiro(): AccountsUiState {
        val contaNu = Conta(
            publicId = "conta-nu",
            instituicao = InstituicaoFinanceira.NUBANK,
            apelido = "Nubank pessoal",
            tipo = TipoConta.CORRENTE,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )
        val contaCaju = Conta(
            publicId = "conta-caju",
            instituicao = InstituicaoFinanceira.CAJU,
            apelido = "Caju",
            tipo = TipoConta.BENEFICIO,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )
        val credOk = Instrumento(
            publicId = "cred-ok",
            contaPublicId = "conta-nu",
            tipo = TipoInstrumento.CREDITO,
            apelido = "Ultravioleta",
            ultimosDigitos = "4321",
            fechamentoDia = 3,
            vencimentoDia = 10,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )
        val credInc = Instrumento(
            publicId = "cred-inc",
            contaPublicId = "conta-nu",
            tipo = TipoInstrumento.CREDITO,
            apelido = "Cartão sem datas",
            ultimosDigitos = "9876",
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )
        val debito = Instrumento(
            publicId = "deb-1",
            contaPublicId = "conta-nu",
            tipo = TipoInstrumento.DEBITO,
            apelido = "Conta",
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )
        val voucher = Instrumento(
            publicId = "vou-1",
            contaPublicId = "conta-caju",
            tipo = TipoInstrumento.VOUCHER,
            apelido = "Refeição",
            beneficio = BeneficioVoucher.VR,
            saldoCentavos = 48_620L,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )

        val grupos = agruparContasFinanceiro(
            listOf(
                ContaComIndicadores(
                    conta = contaNu,
                    instrumentos = listOf(
                        IndicadorDeInstrumento.Credito(
                            instrumento = credOk,
                            estadoFatura = EstadoFaturaCartao.Configurada(
                                faturaPublicId = "fat-1",
                                competencia = LocalDate.of(2026, 11, 1),
                                dataFechamento = LocalDate.of(2026, 11, 3),
                                dataVencimento = LocalDate.of(2026, 11, 10),
                                totalCentavos = 128_450L,
                                status = StatusFatura.ABERTA,
                                diasParaVencimento = 19L,
                                vencida = false,
                            ),
                        ),
                        IndicadorDeInstrumento.Credito(
                            instrumento = credInc,
                            estadoFatura = EstadoFaturaCartao.ConfiguracaoIncompleta,
                        ),
                        IndicadorDeInstrumento.Debito(
                            instrumento = debito,
                            gastoDoMesCentavos = 102_000L,
                            competenciaMes = YearMonth.of(2026, 11),
                        ),
                    ),
                ),
                ContaComIndicadores(
                    conta = contaCaju,
                    instrumentos = listOf(
                        IndicadorDeInstrumento.Voucher(
                            instrumento = voucher,
                            beneficio = BeneficioVoucher.VR,
                            saldoCentavos = 48_620L,
                            ultimaRecarga = UltimaRecargaVoucher(LocalDate.of(2026, 11, 1), 80_000L, prevista = false),
                        ),
                    ),
                ),
            ),
        )
        return AccountsUiState(carregando = false, grupos = grupos, hoje = LocalDate.of(2026, 10, 22))
    }

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-10-22T12:00:00Z")
    }
}
