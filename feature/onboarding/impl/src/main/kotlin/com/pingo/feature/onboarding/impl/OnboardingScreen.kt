package com.pingo.feature.onboarding.impl

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.OnboardingStepper
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoCheckbox
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoMascot
import com.pingo.core.designsystem.component.PingoSpinner
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.PrivacyTermDialog

@Composable
fun OnboardingScreen(onContinue: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(state.completed) {
        if (state.completed) {
            onContinue()
        }
    }

    OnboardingContent(
        state = state,
        onStart = viewModel::goToLogin,
        onLogin = { activity?.let(viewModel::loginWithGoogle) },
        onToggleConsent = viewModel::toggleConsent,
        onAcceptConsent = viewModel::acceptConsent,
        onShowTerm = viewModel::showTerm,
    )

    if (state.showTerm) {
        PrivacyTermDialog(onDismiss = viewModel::dismissTerm)
    }
}

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onStart: () -> Unit,
    onLogin: () -> Unit,
    onToggleConsent: (Boolean) -> Unit,
    onAcceptConsent: () -> Unit,
    onShowTerm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        OnboardingStepper(
            currentStep = state.currentStep,
            totalSteps = state.totalSteps,
        )

        when (state.currentStep) {
            WELCOME_STEP -> WelcomeStep(onStart = onStart)
            LOGIN_STEP -> LoginStep(state = state, onLogin = onLogin)
            else -> ConsentStep(
                state = state,
                onToggleConsent = onToggleConsent,
                onAcceptConsent = onAcceptConsent,
                onShowTerm = onShowTerm,
            )
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        PingoMascot(width = 182.dp)

        PingoText(
            text = "Seu dinheiro,\ngota a gota",
            style = PingoTheme.typography.display,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        PingoText(
            text = "O Pingo lê as notificações dos seus bancos e registra cada compra, Pix e recebimento sozinho. Sem digitar nada.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        PingoButton(
            text = "Começar",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true,
        )
    }
}

@Composable
private fun LoginStep(state: OnboardingUiState, onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        LoginHero()

        GoogleSignInButton(
            loading = state.login == LoginUiState.Loading,
            onClick = onLogin,
        )

        if (state.login == LoginUiState.Error) {
            LoginErrorMessage()
        }

        Spacer(modifier = Modifier.weight(1f))

        PrivacyHint()
    }
}

@Composable
private fun LoginHero() {
    Column(
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PingoMascot(width = 115.dp)

        PingoText(
            text = "Entre com sua conta Google",
            style = PingoTheme.typography.headline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        PingoText(
            text = "Sua conta guarda e restaura seus dados se você trocar de celular.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoginErrorMessage() {
    PingoText(
        text = "Não foi possível entrar, tente de novo",
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.expense,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun GoogleSignInButton(loading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(PingoRadius.md)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape)
            .background(color = PingoTheme.colors.surface, shape = shape)
            .border(width = 1.5.dp, color = PingoTheme.colors.outline, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            PingoSpinner(size = 20.dp)
        } else {
            PingoText(
                text = "G",
                style = PingoTheme.typography.headline.copy(fontWeight = FontWeight.Black),
                color = GoogleBlue,
            )
        }

        Spacer(modifier = Modifier.size(PingoSpacing.md))

        PingoText(
            text = "Continuar com Google",
            style = PingoTheme.typography.label,
            color = PingoTheme.colors.onBackground,
        )
    }
}

@Composable
private fun PrivacyHint() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Lock,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(PingoSpacing.sm))

        PingoText(
            text = "Usamos a conta só para sincronizar e restaurar.",
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConsentStep(
    state: OnboardingUiState,
    onToggleConsent: (Boolean) -> Unit,
    onAcceptConsent: () -> Unit,
    onShowTerm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        ) {
            PingoText(
                text = "Antes de começar",
                style = PingoTheme.typography.headline,
            )
            PingoText(
                text = "O Pingo lê dados sensíveis. Veja com clareza o que acontece com eles.",
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )

            ConsentInfoCard()

            PingoCheckbox(
                checked = state.consentChecked,
                onCheckedChange = onToggleConsent,
                label = "Li e aceito o termo de privacidade (LGPD)",
            )

            PingoButton(
                text = "Ler termo completo",
                onClick = onShowTerm,
                type = PingoButtonType.Text,
                size = PingoButtonSize.Small,
            )
        }

        ConsentActions(state = state, onAcceptConsent = onAcceptConsent)
    }
}

@Composable
private fun ConsentActions(state: OnboardingUiState, onAcceptConsent: () -> Unit) {
    if (state.consent == ConsentUiState.Error) {
        PingoText(
            text = "Não foi possível registrar seu consentimento. Verifique a conexão e tente de novo.",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.expense,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    PingoButton(
        text = "Aceitar e continuar",
        onClick = onAcceptConsent,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.consentChecked,
        loading = state.consent == ConsentUiState.Loading,
    )
}

@Composable
private fun ConsentInfoCard() {
    PingoCard(
        modifier = Modifier.pingoElevation(
            level = PingoElevationLevel.Level1,
            shape = RoundedCornerShape(PingoRadius.xl),
        ),
    ) {
        InformacaoComIcone(
            icon = PingoIcons.ShieldCheck,
            titulo = "O que o Pingo lê",
            descricao = "Só notificações de Nubank, Inter, Itaú, Mercado Pago e Caju. Nada de mensagens, fotos ou contatos.",
        )
        InformacaoComIcone(
            icon = PingoIcons.Smartphone,
            titulo = "O texto fica no seu celular",
            descricao = "A leitura acontece no aparelho. Só dados estruturados são sincronizados, e só com seu consentimento.",
        )
        InformacaoComIcone(
            icon = PingoIcons.Trash2,
            titulo = "Você manda nos seus dados",
            descricao = "Exclua tudo, no aparelho e na nuvem, quando quiser.",
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this

    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }

        current = current.baseContext
    }

    return null
}

private const val WELCOME_STEP = 1
private const val LOGIN_STEP = 2

private val GoogleBlue = androidx.compose.ui.graphics.Color(0xFF4285F4)
