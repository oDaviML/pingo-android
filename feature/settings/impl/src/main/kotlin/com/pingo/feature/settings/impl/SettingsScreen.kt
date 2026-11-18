package com.pingo.feature.settings.impl

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.PingoActionDialog
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoBottomSheet
import com.pingo.core.designsystem.component.PingoDestructiveDialog
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.IntentsDeSistema
import com.pingo.core.ui.PrivacyTermDialog

interface PermissaoDeNotificacaoSettings {
    fun concedida(): Boolean

    fun solicitar(aoResultado: (Boolean) -> Unit)
}

class PermissaoDeNotificacaoSettingsDoAndroid(
    private val context: Context,
    private val precisaPedir: () -> Boolean = { semPermissaoDeNotificacao(context) },
    private val notificacoesHabilitadas: () -> Boolean = { NotificationManagerCompat.from(context).areNotificationsEnabled() },
) : PermissaoDeNotificacaoSettings {
    private var lancador: ((String) -> Unit)? = null
    private var pendente: ((Boolean) -> Unit)? = null

    fun conectarLancador(lancador: (String) -> Unit) {
        this.lancador = lancador
    }

    override fun concedida(): Boolean = !precisaPedir() && notificacoesHabilitadas()

    override fun solicitar(aoResultado: (Boolean) -> Unit) {
        if (!precisaPedir()) {
            aoResultado(concedida())
            return
        }

        val lancar = lancador
        if (lancar == null) {
            aoResultado(concedida())
            return
        }

        pendente = aoResultado
        lancar(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun aoResultadoDaSolicitacao(concedida: Boolean) {
        val resultado = pendente
        pendente = null
        resultado?.invoke(concedida && notificacoesHabilitadas())
    }
}

private fun semPermissaoDeNotificacao(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

@Composable
private fun rememberPermissaoDeNotificacaoSettings(): PermissaoDeNotificacaoSettings {
    val context = LocalContext.current
    val permissao = remember(context) { PermissaoDeNotificacaoSettingsDoAndroid(context) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = permissao::aoResultadoDaSolicitacao,
    )

    LaunchedEffect(permissao, launcher) {
        permissao.conectarLancador(launcher::launch)
    }

    return permissao
}

@Suppress("LongParameterList")
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    permissaoDeNotificacao: PermissaoDeNotificacaoSettings = rememberPermissaoDeNotificacaoSettings(),
    onLoggedOut: () -> Unit = onAccountDeleted,
    onAbrirCategoriasERegras: () -> Unit = {},
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTerm by remember { mutableStateOf(false) }
    var permissaoConcedida by remember(permissaoDeNotificacao) { mutableStateOf(permissaoDeNotificacao.concedida()) }

    SincronizarAoRetomar(permissaoDeNotificacao, viewModel) { concedida ->
        permissaoConcedida = concedida
        viewModel.sincronizarPermissaoDeNotificacao(concedida)
    }

    NavegarAoSairOuExcluir(state = state, onAccountDeleted = onAccountDeleted, onLoggedOut = onLoggedOut)

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsContent(
            onBack = onBack,
            onShowTerm = { showTerm = true },
            onDeleteAccount = viewModel::showDeleteDialog,
            notificacoes = criarNotificacoesUi(state, permissaoConcedida, permissaoDeNotificacao, viewModel, context) {
                permissaoConcedida = it
            },
            conta = SettingsContaUi(state.nomeUsuario, state.emailUsuario, viewModel::solicitarLogout),
            seguranca = criarSegurancaUi(state, viewModel, context, onAbrirCategoriasERegras),
            privacidade = SettingsPrivacidadeUi(
                telemetriaOptIn = state.telemetriaOptIn,
                onToggleTelemetria = viewModel::alternarTelemetria,
                onShowTerm = { showTerm = true },
                onDeleteAccount = viewModel::showDeleteDialog,
            ),
            sobre = criarSobreUi(state, viewModel, context),
        )
        if (state.mostrarSheetTempoBloqueio) {
            BloquearAposBottomSheet(
                selecionado = OpcaoBloqueio.deSegundos(state.tempoBloqueioSegundos),
                onSelecionar = viewModel::selecionarTempoBloqueio,
                onDismissRequest = viewModel::fecharSheetTempoBloqueio,
            )
        }
    }

    SettingsDialogs(showTerm = showTerm, onDismissTerm = { showTerm = false }, state = state, viewModel = viewModel)
}

@Composable
private fun NavegarAoSairOuExcluir(state: SettingsUiState, onAccountDeleted: () -> Unit, onLoggedOut: () -> Unit) {
    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) {
            onAccountDeleted()
        }
    }
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) {
            onLoggedOut()
        }
    }
}

@Composable
private fun SincronizarAoRetomar(
    permissaoDeNotificacao: PermissaoDeNotificacaoSettings,
    viewModel: SettingsViewModel,
    aoAtualizarPermissao: (Boolean) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permissaoDeNotificacao, viewModel) {
        val observador = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                aoAtualizarPermissao(permissaoDeNotificacao.concedida())
                viewModel.atualizarDisponibilidadeBiometria()
                viewModel.atualizarPermissaoInstalacao()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }
}

private fun criarNotificacoesUi(
    state: SettingsUiState,
    permissaoConcedida: Boolean,
    permissaoDeNotificacao: PermissaoDeNotificacaoSettings,
    viewModel: SettingsViewModel,
    context: Context,
    atualizarPermissao: (Boolean) -> Unit,
): SettingsNotificacoesUi = SettingsNotificacoesUi(
    avisoAssinaturasAtivo = state.avisoAssinaturasAtivo && permissaoConcedida,
    permissaoNotificacaoNegada = state.permissaoNotificacaoNegada,
    onToggleAvisoAssinaturas = { novoValor ->
        tratarToggleAvisoAssinaturas(novoValor, permissaoDeNotificacao, viewModel, atualizarPermissao)
    },
    onAbrirConfiguracoesNotificacao = {
        IntentsDeSistema.abrir(context, IntentsDeSistema.notificacoesDoApp(context.packageName))
    },
    revisarPelaNotificacaoAtivo = state.revisarPelaNotificacaoAtivo,
    onToggleRevisarPelaNotificacao = viewModel::alternarRevisarPelaNotificacao,
    alertaCapturaParadaAtivo = state.alertaCapturaParadaAtivo,
    onToggleAlertaCapturaParada = viewModel::alternarAlertaCapturaParada,
)

private fun criarSegurancaUi(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    context: Context,
    onAbrirCategoriasERegras: () -> Unit,
): SettingsSegurancaUi = SettingsSegurancaUi(
    biometriaAtiva = state.biometriaAtiva,
    disponibilidadeBiometria = state.disponibilidadeBiometria,
    tempoBloqueio = OpcaoBloqueio.deSegundos(state.tempoBloqueioSegundos),
    mensagemSeguranca = state.mensagemSeguranca,
    mostrarAtalhoCadastroBiometria = state.mostrarAtalhoCadastroBiometria,
    onToggleBiometria = { ativo -> viewModel.alternarBiometria(ativo, context.encontrarActivity()) },
    onAbrirTempoBloqueio = viewModel::abrirSheetTempoBloqueio,
    onAbrirConfiguracoesSeguranca = { context.encontrarActivity()?.let(viewModel::abrirConfiguracoesDeSeguranca) },
    onAbrirCategoriasERegras = onAbrirCategoriasERegras,
)

private fun criarSobreUi(state: SettingsUiState, viewModel: SettingsViewModel, context: Context): SettingsSobreUi = SettingsSobreUi(
    versaoApp = state.versaoApp,
    codigoVersaoApp = state.codigoVersaoApp,
    estadoAtualizacao = state.estadoAtualizacao,
    onVerificarAtualizacao = viewModel::verificarAtualizacao,
    onBaixarAtualizacao = viewModel::baixarAtualizacao,
    onInstalarAtualizacao = { arquivo -> runCatching { context.startActivity(viewModel.intentInstalacaoApk(arquivo)) } },
    onAbrirPermissaoFontes = { runCatching { context.startActivity(viewModel.intentPermissaoFontesDesconhecidas()) } },
    onAbrirPaginaRelease = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
)

private fun tratarToggleAvisoAssinaturas(
    novoValor: Boolean,
    permissaoDeNotificacao: PermissaoDeNotificacaoSettings,
    viewModel: SettingsViewModel,
    onPermissaoAtualizada: (Boolean) -> Unit,
) {
    if (!novoValor) {
        viewModel.alternarAvisoAssinaturas(false)
        return
    }
    if (permissaoDeNotificacao.concedida()) {
        onPermissaoAtualizada(true)
        viewModel.alternarAvisoAssinaturas(true)
        return
    }
    permissaoDeNotificacao.solicitar { concedida ->
        onPermissaoAtualizada(concedida)
        if (concedida) viewModel.alternarAvisoAssinaturas(true) else viewModel.aoNegarPermissaoDeNotificacao()
    }
}

@Composable
private fun SettingsDialogs(showTerm: Boolean, onDismissTerm: () -> Unit, state: SettingsUiState, viewModel: SettingsViewModel) {
    if (showTerm) {
        PrivacyTermDialog(onDismiss = onDismissTerm)
    }
    if (state.showLogoutDialog) {
        PingoActionDialog(
            title = "Sair da conta?",
            description = "Seus lançamentos e configurações continuam salvos neste aparelho.",
            confirmText = "Sair",
            onConfirm = viewModel::confirmarLogout,
            onDismissRequest = viewModel::cancelarLogout,
            confirmLoading = state.saindoDaConta,
        )
    }
    if (state.showDeleteDialog) {
        PingoDestructiveDialog(
            title = "Excluir conta e dados?",
            description = deleteDescription(failed = state.deleteFailed),
            confirmText = "Excluir",
            onConfirm = viewModel::deleteAccount,
            onDismissRequest = viewModel::dismissDeleteDialog,
            confirmLoading = state.deleting,
        )
    }
}

@Composable
private fun BloquearAposBottomSheet(selecionado: OpcaoBloqueio, onSelecionar: (OpcaoBloqueio) -> Unit, onDismissRequest: () -> Unit) {
    PingoBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "Bloquear após",
        subtitle = "Escolha quanto tempo o Pingo aguarda em segundo plano.",
    ) {
        OpcaoBloqueio.entries.forEach { opcao ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PingoLayout.minTouchTarget)
                    .selectable(selected = opcao == selecionado, role = Role.RadioButton, onClick = { onSelecionar(opcao) })
                    .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PingoText(text = opcao.rotulo, style = PingoTheme.typography.body)
                if (opcao == selecionado) {
                    PingoIcon(
                        imageVector = PingoIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PingoTheme.colors.primary,
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
fun SettingsContent(
    onBack: () -> Unit,
    onShowTerm: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    notificacoes: SettingsNotificacoesUi = SettingsNotificacoesUi(),
    conta: SettingsContaUi = SettingsContaUi(),
    seguranca: SettingsSegurancaUi = SettingsSegurancaUi(),
    privacidade: SettingsPrivacidadeUi = SettingsPrivacidadeUi(onShowTerm = onShowTerm, onDeleteAccount = onDeleteAccount),
    sobre: SettingsSobreUi = SettingsSobreUi(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        PingoTopBar(title = "Configurações", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            SectionHeader("Conta")
            ContaCartao(conta = conta)
            SectionHeader("Segurança e categorias")
            SegurancaCartao(seguranca = seguranca)
            SectionHeader("Notificações do Pingo")
            NotificacoesCartao(notificacoes = notificacoes)
            if (notificacoes.permissaoNotificacaoNegada) {
                PingoBanner(
                    title = "Permissão de notificações bloqueada",
                    description = "Ative as notificações nas configurações do aparelho para receber avisos de assinaturas.",
                    tone = PingoBannerTone.Warning,
                    onClick = notificacoes.onAbrirConfiguracoesNotificacao,
                )
            }
            SectionHeader("Privacidade e dados")
            PrivacidadeCartao(privacidade = privacidade.copy(onShowTerm = onShowTerm, onDeleteAccount = onDeleteAccount))
            SectionHeader("Sobre o Pingo")
            SobreCartao(sobre = sobre)
        }
    }
}

private fun deleteDescription(failed: Boolean): String {
    val base = "Isso apaga tudo no celular e na nuvem e não dá para desfazer."

    return if (failed) {
        "$base\n\nNão foi possível excluir. Verifique a conexão e tente de novo."
    } else {
        base
    }
}

private tailrec fun Context.encontrarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.encontrarActivity()
    else -> null
}
