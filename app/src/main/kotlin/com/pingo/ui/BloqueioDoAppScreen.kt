package com.pingo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun BloqueioDoAppScreen(
    mensagemErro: String?,
    autenticando: Boolean,
    onDesbloquear: () -> Unit,
    onSairDaConta: () -> Unit,
    onFecharApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onFecharApp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(PingoSpacing.xxl),
    ) {
        BloqueioConteudoCentral(
            mensagemErro = mensagemErro,
            autenticando = autenticando,
            onDesbloquear = onDesbloquear,
            onSairDaConta = onSairDaConta,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun BloqueioConteudoCentral(
    mensagemErro: String?,
    autenticando: Boolean,
    onDesbloquear: () -> Unit,
    onSairDaConta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        BloqueioEmblema()
        PingoText(text = "Pingo bloqueado", style = PingoTheme.typography.headline, textAlign = TextAlign.Center)
        PingoText(
            text = "Confirme com biometria ou senha do celular para continuar.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        mensagemErro?.let { erro ->
            PingoText(
                text = erro,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.expense,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(modifier = Modifier.height(PingoSpacing.sm))
        BloqueioAcoes(autenticando = autenticando, onDesbloquear = onDesbloquear, onSairDaConta = onSairDaConta)
    }
}

@Composable
private fun BloqueioAcoes(autenticando: Boolean, onDesbloquear: () -> Unit, onSairDaConta: () -> Unit) {
    PingoButton(
        text = "Desbloquear",
        onClick = onDesbloquear,
        enabled = !autenticando,
        loading = autenticando,
        modifier = Modifier.fillMaxWidth().testTag("botao_desbloquear_app"),
    )
    PingoButton(
        text = "Sair da conta",
        onClick = onSairDaConta,
        type = PingoButtonType.Text,
        enabled = !autenticando,
        modifier = Modifier.fillMaxWidth().testTag("botao_sair_tela_bloqueio"),
    )
}

@Composable
private fun BloqueioEmblema() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(color = PingoTheme.colors.primaryContainer, shape = RoundedCornerShape(PingoRadius.xl)),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Lock,
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = PingoTheme.colors.primary,
        )
    }
}
