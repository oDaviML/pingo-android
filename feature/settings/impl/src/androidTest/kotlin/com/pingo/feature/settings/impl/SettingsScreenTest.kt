package com.pingo.feature.settings.impl

import android.Manifest
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.AccountRepository
import com.pingo.core.data.assinatura.AgendadorDeAvisosDeAssinatura
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.network.update.AtualizacaoDisponivel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.util.Optional

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mostraSecoesNotificacoesPrivacidadeESobreComTogglesPadrao() {
        composeRule.setContent {
            PingoTheme {
                SettingsContent(
                    onBack = {},
                    onShowTerm = {},
                    onDeleteAccount = {},
                )
            }
        }

        composeRule.onNodeWithText("Configurações").assertIsDisplayed()
        composeRule.onNodeWithText("Conta").assertIsDisplayed()
        composeRule.onNodeWithText("Segurança e categorias").assertIsDisplayed()
        composeRule.onNodeWithText("Notificações do Pingo").performScrollTo().assertIsDisplayed()
        composeRule.onNode(isToggleable() and hasText("Avisos de assinaturas")).performScrollTo().assertIsOff()
        composeRule.onNode(isToggleable() and hasText("Telemetria anônima")).performScrollTo().assertIsOff()
        composeRule.onNodeWithText("Privacidade e dados").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Termo de privacidade").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Excluir conta e dados").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sobre o Pingo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Procurar atualização").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun exibeAvisoQuandoPermissaoNegadaEAbreConfiguracoes() {
        var abriuConfiguracoes = 0

        composeRule.setContent {
            PingoTheme {
                SettingsContent(
                    onBack = {},
                    onShowTerm = {},
                    onDeleteAccount = {},
                    notificacoes = SettingsNotificacoesUi(
                        avisoAssinaturasAtivo = false,
                        permissaoNotificacaoNegada = true,
                        onAbrirConfiguracoesNotificacao = { abriuConfiguracoes++ },
                    ),
                )
            }
        }

        composeRule.onNode(isToggleable() and hasText("Avisos de assinaturas")).performScrollTo().assertIsOff()
        composeRule.onNodeWithText("Permissão de notificações bloqueada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Permissão de notificações bloqueada").performClick()

        assertEquals(1, abriuConfiguracoes)
    }

    @Test
    fun negarPostNotificationsMantemToggleDesligadoEMostraAviso() {
        val preferences = FakePreferences()
        val agendador = FakeAgendador()
        val viewModel = SettingsViewModel(
            accountRepository = FakeAccountRepository(),
            preferences = preferences,
            agendadorDeAvisos = AgendadorDeAvisosOpcional(Optional.of(agendador)),
        )
        val permissao = FakePermissao(concedida = false, resultadoSolicitacao = false)

        composeRule.setContent {
            PingoTheme {
                SettingsScreen(
                    onBack = {},
                    onAccountDeleted = {},
                    viewModel = viewModel,
                    permissaoDeNotificacao = permissao,
                )
            }
        }

        composeRule.onNodeWithText("Avisos de assinaturas").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, permissao.solicitacoes)
        assertFalse(preferences.estado.value)
        assertEquals(0, agendador.reconciliacoes)
        composeRule.onNode(isToggleable() and hasText("Avisos de assinaturas")).assertIsOff()
        composeRule.onNodeWithText("Permissão de notificações bloqueada").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun concederPermissaoLigaToggleEReconcilia() {
        val preferences = FakePreferences()
        val agendador = FakeAgendador()
        val viewModel = SettingsViewModel(
            accountRepository = FakeAccountRepository(),
            preferences = preferences,
            agendadorDeAvisos = AgendadorDeAvisosOpcional(Optional.of(agendador)),
        )
        val permissao = FakePermissao(concedida = false, resultadoSolicitacao = true)

        composeRule.setContent {
            PingoTheme {
                SettingsScreen(
                    onBack = {},
                    onAccountDeleted = {},
                    viewModel = viewModel,
                    permissaoDeNotificacao = permissao,
                )
            }
        }

        composeRule.onNodeWithText("Avisos de assinaturas").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, permissao.solicitacoes)
        assertTrue(preferences.estado.value)
        assertEquals(1, agendador.reconciliacoes)
        composeRule.onNode(isToggleable() and hasText("Avisos de assinaturas")).assertIsOn()
        composeRule.onNodeWithText("Permissão de notificações bloqueada").assertDoesNotExist()
    }

    @Test
    fun solicitacaoDePermissaoPedePostNotifications() {
        val contexto: Context = ApplicationProvider.getApplicationContext()
        val permissao = PermissaoDeNotificacaoSettingsDoAndroid(
            context = contexto,
            precisaPedir = { true },
            notificacoesHabilitadas = { false },
        )
        var pedida: String? = null
        var resultado: Boolean? = null

        permissao.conectarLancador { pedida = it }
        permissao.solicitar { resultado = it }

        assertEquals(Manifest.permission.POST_NOTIFICATIONS, pedida)

        permissao.aoResultadoDaSolicitacao(concedida = false)

        assertEquals(false, resultado)
    }

    @Test
    fun abreConfirmacaoDeExclusaoESolicitaSairECategorias() {
        var deleteClicks = 0
        var logoutClicks = 0
        var categoriasClicks = 0

        composeRule.setContent {
            PingoTheme {
                SettingsContent(
                    onBack = {},
                    onShowTerm = {},
                    onDeleteAccount = { deleteClicks++ },
                    conta = SettingsContaUi("Ana", "ana@pingo.app", onSolicitarLogout = { logoutClicks++ }),
                    seguranca = SettingsSegurancaUi(onAbrirCategoriasERegras = { categoriasClicks++ }),
                )
            }
        }

        composeRule.onNodeWithText("Ana").assertIsDisplayed()
        composeRule.onNodeWithText("ana@pingo.app").assertIsDisplayed()
        composeRule.onNodeWithText("Sair da conta").performScrollTo().performClick()
        composeRule.onNodeWithText("Categorias e regras aprendidas").performScrollTo().performClick()
        composeRule.onNodeWithText("Excluir conta e dados").performScrollTo().performClick()

        assertEquals(1, logoutClicks)
        assertEquals(1, categoriasClicks)
        assertEquals(1, deleteClicks)
    }

    @Test
    fun exibeEstadosDeAtualizacaoApkBaixarEInstalar() {
        var baixou = 0
        var instalou = 0
        val info = AtualizacaoDisponivel(
            "0.3.0",
            "v0.3.0",
            "Beta",
            "Notas",
            "https://github.com/o/r",
            "https://example.com/a.apk",
            "pingo.apk",
        )

        composeRule.setContent {
            PingoTheme {
                SettingsContent(
                    onBack = {},
                    onShowTerm = {},
                    onDeleteAccount = {},
                    sobre = SettingsSobreUi(
                        estadoAtualizacao = EstadoAtualizacaoAppUi.ProntoParaInstalar(
                            info,
                            File("pingo.apk"),
                            precisaPermissaoFontes = false,
                        ),
                        onBaixarAtualizacao = { baixou++ },
                        onInstalarAtualizacao = { instalou++ },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("botao_instalar_atualizacao").performScrollTo().performClick()

        assertEquals(0, baixou)
        assertEquals(1, instalou)
    }

    private class FakePermissao(private var concedida: Boolean, private val resultadoSolicitacao: Boolean) :
        PermissaoDeNotificacaoSettings {
        var solicitacoes = 0

        override fun concedida(): Boolean = concedida

        override fun solicitar(aoResultado: (Boolean) -> Unit) {
            solicitacoes++
            concedida = resultadoSolicitacao
            aoResultado(resultadoSolicitacao)
        }
    }

    private class FakeAccountRepository : AccountRepository {
        override suspend fun recordConsent(version: String) = Unit

        override suspend fun deleteAccount() = Unit
    }

    private class FakeAgendador : AgendadorDeAvisosDeAssinatura {
        var reconciliacoes = 0
        var cancelamentos = 0

        override suspend fun reconciliarTodas() {
            reconciliacoes++
        }

        override suspend fun cancelarParaAssinatura(assinaturaPublicId: String) = Unit

        override suspend fun cancelarTodos() {
            cancelamentos++
        }
    }

    private class FakePreferences : UserPreferencesDataSource {
        val estado = MutableStateFlow(false)
        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)
        override val avisoAssinaturasAtivo: Flow<Boolean> = estado

        override suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) {
            estado.value = ativo
        }

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev"

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }
}
