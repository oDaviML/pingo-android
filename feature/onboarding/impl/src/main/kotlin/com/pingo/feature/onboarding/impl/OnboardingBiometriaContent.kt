package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.designsystem.component.OnboardingStepper
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

private const val PASSO_BIOMETRIA = 8
private const val TOTAL_PASSOS_BIOMETRIA = 8

@Composable
fun OnboardingBiometriaContent(
    state: OnboardingBiometriaUiState,
    onAtivar: () -> Unit,
    onAgoraNao: () -> Unit,
    onAbrirConfiguracoesDeSeguranca: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        OnboardingStepper(
            currentStep = PASSO_BIOMETRIA,
            totalSteps = TOTAL_PASSOS_BIOMETRIA,
            onSkip = onAgoraNao,
        )

        BiometriaBody(
            state = state,
            onAbrirConfiguracoesDeSeguranca = onAbrirConfiguracoesDeSeguranca,
            modifier = Modifier.weight(1f),
        )

        BiometriaActions(
            state = state,
            onAtivar = onAtivar,
            onAgoraNao = onAgoraNao,
        )
    }
}

@Composable
private fun BiometriaBody(state: OnboardingBiometriaUiState, onAbrirConfiguracoesDeSeguranca: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = PingoTheme.colors.primaryContainer,
                    shape = RoundedCornerShape(PingoRadius.lg),
                ),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = PingoIcons.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = PingoTheme.colors.primary,
            )
        }

        PingoText(
            text = "Proteja seus gastos com biometria",
            style = PingoTheme.typography.headline,
        )

        PingoText(
            text = "Peça digital, rosto ou senha do aparelho antes de mostrar seus lançamentos e saldos.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )

        if (state.semCadastroNoAparelho) {
            SemBiometriaCard(onAbrirConfiguracoesDeSeguranca = onAbrirConfiguracoesDeSeguranca)
        }

        BiometriaInfoCard()
    }
}

@Composable
private fun BiometriaInfoCard() {
    PingoCard(modifier = Modifier.fillMaxWidth()) {
        InformacaoComIcone(
            icon = PingoIcons.Lock,
            titulo = "Privacidade na troca de apps",
            descricao = "Quando o Pingo estiver bloqueado, o Android oculta valores sensíveis nas telas recentes.",
        )
        InformacaoComIcone(
            icon = PingoIcons.ShieldCheck,
            titulo = "Controle flexível",
            descricao = "Você pode ativar, desligar ou ajustar o tempo para bloquear quando quiser em Configurações.",
        )
    }
}

@Composable
private fun SemBiometriaCard(onAbrirConfiguracoesDeSeguranca: () -> Unit) {
    PingoCard(modifier = Modifier.fillMaxWidth()) {
        PingoText(
            text = "Nenhuma biometria ou senha está configurada",
            style = PingoTheme.typography.bodyStrong,
        )
        PingoText(
            text = "Cadastre sua digital ou PIN nas configurações do Android para usar este bloqueio.",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoButton(
            text = "Configurar no aparelho",
            onClick = onAbrirConfiguracoesDeSeguranca,
            type = PingoButtonType.Outline,
            modifier = Modifier.fillMaxWidth().testTag("botao_configurar_biometria_aparelho"),
        )
    }
}

@Composable
private fun BiometriaActions(state: OnboardingBiometriaUiState, onAtivar: () -> Unit, onAgoraNao: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        state.mensagemErro?.let { mensagem ->
            PingoText(
                text = mensagem,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.expense,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        val podeAtivar = state.disponibilidade != DisponibilidadeAutenticacao.INDISPONIVEL && !state.concluido

        PingoButton(
            text = "Ativar biometria",
            onClick = onAtivar,
            modifier = Modifier.fillMaxWidth().testTag("botao_ativar_biometria"),
            enabled = podeAtivar,
            loading = state.autenticando,
        )

        PingoButton(
            text = "Agora não",
            onClick = onAgoraNao,
            type = PingoButtonType.Text,
            modifier = Modifier.fillMaxWidth().testTag("botao_agora_nao_biometria"),
            enabled = !state.autenticando && !state.concluido,
        )
    }
}
