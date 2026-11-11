package com.pingo.feature.capturestatus.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.notifications.SituacaoDaCaptura
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CaptureStatusContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-10T15:30:00Z")

    @Test
    fun estadoAtivoMostraEstadoContadoresLeiturasEPermissoes() {
        composeRule.setContent {
            PingoTheme {
                CaptureStatusContent(
                    state = estado(
                        ultimaLeituraEm = agora.minus(Duration.ofMinutes(3)),
                        ultimaLeituraInstituicao = InstituicaoFinanceira.ITAU,
                        contadores = ContadoresDaCaptura(reconhecidas = 184, paraRevisao = 12, ignoradas = 37, naoEntendidas = 3),
                        leituras = leiturasDe(InstituicaoFinanceira.ITAU to agora.minus(Duration.ofMinutes(3))),
                    ),
                    onBack = {},
                    onReconectar = {},
                    onAtualizarRegras = {},
                    onAbrirAjuste = {},
                )
            }
        }

        composeRule.onNodeWithText("Captura ativa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Lendo as notificações dos seus apps financeiros").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Última leitura há 3 min, do Itaú.").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Neste mês").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("184").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reconhecidas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("12").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Para revisão").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("37").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ignoradas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Não entendidas").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Última notificação por app").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nubank").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Itaú").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mercado Pago").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Caju").performScrollTo().assertIsDisplayed()

        composeRule.onAllNodesWithText("Nunca").assertCountEquals(4)

        composeRule.onNodeWithText("Acesso a notificações").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ativo — lendo notificações dos bancos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Notificações do Pingo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Restrição de bateria").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Versão 1 · atualizadas há 2 dias").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Atualizar regras").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reconectar").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Corrigir permissão").assertCountEquals(0)
    }

    @Test
    fun estadoParadoMostraMotivoEAcoes() {
        var reconexoes = 0
        val ajustes = mutableListOf<ItemDePermissao>()

        composeRule.setContent {
            PingoTheme {
                CaptureStatusContent(
                    state = estado(
                        situacao = SituacaoDaCaptura.PARADA,
                        motivo = MotivoParada.PERMISSAO_REVOGADA,
                        permissoes = permissoes(acesso = false),
                        versao = null,
                        regrasAtualizadasEm = null,
                    ),
                    onBack = {},
                    onReconectar = { reconexoes++ },
                    onAtualizarRegras = {},
                    onAbrirAjuste = { ajustes += it },
                )
            }
        }

        composeRule.onNodeWithText("Captura parada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Desativado — o Pingo não consegue ler").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Versão desconhecida · nunca atualizadas").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Corrigir").performScrollTo().performClick()
        composeRule.onNodeWithText("Corrigir permissão").performScrollTo().performClick()
        composeRule.onNodeWithText("Reconectar").performScrollTo().performClick()

        assertEquals(listOf(ItemDePermissao.ACESSO_A_NOTIFICACOES, ItemDePermissao.ACESSO_A_NOTIFICACOES), ajustes.toList())
        assertEquals(1, reconexoes)
    }

    @Test
    fun semLeituraHaDoisDiasSinalizaCapturaParada() {
        composeRule.setContent {
            PingoTheme {
                CaptureStatusContent(
                    state = estado(
                        situacao = SituacaoDaCaptura.SEM_LEITURA,
                        ultimaLeituraEm = agora.minus(Duration.ofDays(2)),
                        ultimaLeituraInstituicao = InstituicaoFinanceira.NUBANK,
                        semLeituraDesde = agora.minus(Duration.ofDays(2)),
                        leituras = leiturasDe(InstituicaoFinanceira.NUBANK to agora.minus(Duration.ofDays(2))),
                    ),
                    onBack = {},
                    onReconectar = {},
                    onAtualizarRegras = {},
                    onAbrirAjuste = {},
                )
            }
        }

        composeRule.onNodeWithText("Captura parada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem leitura há 2 dias").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Compras feitas agora podem não aparecer. Confira as permissões abaixo e reconecte.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pendenteSemInstrumentoApareceParaRevisaoSemAvisoDeParada() {
        composeRule.setContent {
            PingoTheme {
                CaptureStatusContent(
                    state = estado(
                        ultimaLeituraEm = agora.minus(Duration.ofMinutes(1)),
                        ultimaLeituraInstituicao = InstituicaoFinanceira.INTER,
                        contadores = ContadoresDaCaptura(paraRevisao = 1),
                        leituras = leiturasDe(InstituicaoFinanceira.INTER to agora.minus(Duration.ofMinutes(1))),
                    ),
                    onBack = {},
                    onReconectar = {},
                    onAtualizarRegras = {},
                    onAbrirAjuste = {},
                )
            }
        }

        composeRule.onNodeWithText("Captura ativa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Para revisão").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Captura parada").assertCountEquals(0)
    }

    @Test
    fun falhaAoAtualizarRegrasApareceNoCartao() {
        composeRule.setContent {
            PingoTheme {
                CaptureStatusContent(
                    state = estado(falhaAoAtualizarRegras = "sem_conexao"),
                    onBack = {},
                    onReconectar = {},
                    onAtualizarRegras = {},
                    onAbrirAjuste = {},
                )
            }
        }

        composeRule.onNodeWithText("Versão 1 · atualizadas há 2 dias · falha: sem conexão").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun atualizarRegrasEmAndamentoNaoDisparaOutraAtualizacao() {
        var atualizacoes = 0

        composeRule.setContent {
            PingoTheme {
                CaptureStatusContent(
                    state = estado(atualizandoRegras = true),
                    onBack = {},
                    onReconectar = {},
                    onAtualizarRegras = { atualizacoes++ },
                    onAbrirAjuste = {},
                )
            }
        }

        composeRule.onNodeWithText("Atualizar regras").performScrollTo().performClick()

        assertEquals(0, atualizacoes)
    }

    private fun estado(
        situacao: SituacaoDaCaptura = SituacaoDaCaptura.ATIVA,
        motivo: MotivoParada? = null,
        ultimaLeituraEm: Instant? = null,
        ultimaLeituraInstituicao: InstituicaoFinanceira? = null,
        semLeituraDesde: Instant? = null,
        contadores: ContadoresDaCaptura = ContadoresDaCaptura(),
        leituras: List<LeituraPorApp> = leiturasDe(),
        permissoes: List<PermissaoDaCaptura> = permissoes(),
        versao: Int? = 1,
        regrasAtualizadasEm: Instant? = agora.minus(Duration.ofDays(2)),
        falhaAoAtualizarRegras: String? = null,
        atualizandoRegras: Boolean = false,
    ) = CaptureStatusUiState(
        carregando = false,
        situacao = situacao,
        motivo = motivo,
        ultimaLeituraEm = ultimaLeituraEm,
        ultimaLeituraInstituicao = ultimaLeituraInstituicao,
        semLeituraDesde = semLeituraDesde,
        agora = agora,
        contadores = contadores,
        leiturasPorApp = leituras,
        permissoes = permissoes,
        versaoDasRegras = versao,
        regrasAtualizadasEm = regrasAtualizadasEm,
        falhaAoAtualizarRegras = falhaAoAtualizarRegras,
        atualizandoRegras = atualizandoRegras,
    )

    private fun leiturasDe(vararg leituras: Pair<InstituicaoFinanceira, Instant>): List<LeituraPorApp> {
        val porInstituicao = leituras.toMap()

        return listOf(
            InstituicaoFinanceira.NUBANK,
            InstituicaoFinanceira.INTER,
            InstituicaoFinanceira.ITAU,
            InstituicaoFinanceira.MERCADO_PAGO,
            InstituicaoFinanceira.CAJU,
        ).map { instituicao -> LeituraPorApp(instituicao, porInstituicao[instituicao]) }
    }

    private fun permissoes(acesso: Boolean = true, notificacoes: Boolean = true, bateria: Boolean = true): List<PermissaoDaCaptura> =
        listOf(
            PermissaoDaCaptura(ItemDePermissao.ACESSO_A_NOTIFICACOES, acesso),
            PermissaoDaCaptura(ItemDePermissao.NOTIFICACOES_DO_PINGO, notificacoes),
            PermissaoDaCaptura(ItemDePermissao.BATERIA, bateria),
        )
}
