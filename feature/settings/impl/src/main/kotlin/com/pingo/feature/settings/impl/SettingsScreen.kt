package com.pingo.feature.settings.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoDestructiveDialog
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.component.SettingsRow
import com.pingo.core.designsystem.component.SettingsRowStyle
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
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

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    permissaoDeNotificacao: PermissaoDeNotificacaoSettings = rememberPermissaoDeNotificacaoSettings(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTerm by remember { mutableStateOf(false) }
    var permissaoConcedida by remember(permissaoDeNotificacao) { mutableStateOf(permissaoDeNotificacao.concedida()) }

    SincronizarPermissaoAoRetomar(permissaoDeNotificacao) { concedida ->
        permissaoConcedida = concedida
        viewModel.sincronizarPermissaoDeNotificacao(concedida)
    }

    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) {
            onAccountDeleted()
        }
    }

    SettingsContent(
        onBack = onBack,
        onShowTerm = { showTerm = true },
        onDeleteAccount = viewModel::showDeleteDialog,
        notificacoes = SettingsNotificacoesUi(
            avisoAssinaturasAtivo = state.avisoAssinaturasAtivo && permissaoConcedida,
            permissaoNotificacaoNegada = state.permissaoNotificacaoNegada,
            onToggleAvisoAssinaturas = { novoValor ->
                tratarToggleAvisoAssinaturas(novoValor, permissaoDeNotificacao, viewModel) { permissaoConcedida = it }
            },
            onAbrirConfiguracoesNotificacao = {
                IntentsDeSistema.abrir(context, IntentsDeSistema.notificacoesDoApp(context.packageName))
            },
        ),
    )

    SettingsDialogs(
        showTerm = showTerm,
        onDismissTerm = { showTerm = false },
        state = state,
        viewModel = viewModel,
    )
}

@Composable
private fun SincronizarPermissaoAoRetomar(permissaoDeNotificacao: PermissaoDeNotificacaoSettings, aoAtualizar: (Boolean) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permissaoDeNotificacao) {
        val observador = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                aoAtualizar(permissaoDeNotificacao.concedida())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }
}

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
        if (concedida) {
            viewModel.alternarAvisoAssinaturas(true)
        } else {
            viewModel.aoNegarPermissaoDeNotificacao()
        }
    }
}

@Composable
private fun SettingsDialogs(showTerm: Boolean, onDismissTerm: () -> Unit, state: SettingsUiState, viewModel: SettingsViewModel) {
    if (showTerm) {
        PrivacyTermDialog(onDismiss = onDismissTerm)
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
fun SettingsContent(
    onBack: () -> Unit,
    onShowTerm: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    notificacoes: SettingsNotificacoesUi = SettingsNotificacoesUi(),
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
            SectionHeader("Notificações do Pingo")
            NotificacoesCartao(
                avisoAssinaturasAtivo = notificacoes.avisoAssinaturasAtivo,
                onToggleAvisoAssinaturas = notificacoes.onToggleAvisoAssinaturas,
            )
            if (notificacoes.permissaoNotificacaoNegada) {
                PingoBanner(
                    title = "Permissão de notificações bloqueada",
                    description = "Ative as notificações nas configurações do aparelho para receber avisos de assinaturas.",
                    tone = PingoBannerTone.Warning,
                    onClick = notificacoes.onAbrirConfiguracoesNotificacao,
                )
            }
            SectionHeader("Privacidade e dados")
            PrivacidadeCartao(onShowTerm = onShowTerm, onDeleteAccount = onDeleteAccount)
        }
    }
}

@Composable
private fun NotificacoesCartao(avisoAssinaturasAtivo: Boolean, onToggleAvisoAssinaturas: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        ToggleSettingsRow(
            title = "Avisos de assinaturas",
            subtitle = "2 dias antes da cobrança estimada",
            checked = avisoAssinaturasAtivo,
            onCheckedChange = onToggleAvisoAssinaturas,
        )
    }
}

@Composable
private fun ToggleSettingsRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleSettingsIcon()
        ToggleSettingsTexts(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        PingoSwitch(checked = checked)
    }
}

@Composable
private fun ToggleSettingsIcon() {
    val colors = PingoTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = colors.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Repeat,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colors.onPrimaryContainer,
        )
    }
}

@Composable
private fun ToggleSettingsTexts(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = PingoTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PingoText(
            text = title,
            style = PingoTheme.typography.bodyStrong,
            color = colors.onBackground,
        )
        PingoText(
            text = subtitle,
            style = PingoTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PingoSwitch(checked: Boolean) {
    val colors = PingoTheme.colors
    val trackModifier = if (checked) {
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .padding(4.dp)
    } else {
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .border(width = 2.dp, color = colors.outline, shape = CircleShape)
            .padding(horizontal = 8.dp)
    }

    Box(
        modifier = trackModifier,
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(if (checked) 24.dp else 16.dp)
                .background(
                    color = if (checked) colors.onPrimary else colors.onSurfaceVariant,
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun PrivacidadeCartao(onShowTerm: () -> Unit, onDeleteAccount: () -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        SettingsRow(
            title = "Termo de privacidade",
            subtitle = "Como o Pingo trata seus dados",
            icon = PingoIcons.FileText,
            onClick = onShowTerm,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PingoTheme.colors.outlineVariant),
        )
        SettingsRow(
            title = "Excluir conta e dados",
            subtitle = "Apaga tudo no aparelho e na nuvem",
            icon = PingoIcons.Trash2,
            onClick = onDeleteAccount,
            style = SettingsRowStyle.Danger,
        )
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
