package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.pingo.core.designsystem.component.OnboardingStepper
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

private const val PASSO_CONTAS = 7
private const val TOTAL_PASSOS = 8
private const val MENSAGEM_SALVAR_CONTAS = "Não foi possível salvar suas contas. Tente de novo."

@Composable
fun OnboardingContasContent(state: OnboardingContasUiState, onEvent: (OnboardingContasEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        OnboardingStepper(
            currentStep = PASSO_CONTAS,
            totalSteps = TOTAL_PASSOS,
            onSkip = { onEvent(OnboardingContasEvent.Pular) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        ) {
            PingoText(
                text = "Quais bancos você usa?",
                style = PingoTheme.typography.headline,
            )
            PingoText(
                text = "O Pingo cria uma conta para cada banco e os cartões ou benefícios que você escolher.",
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )

            BancosEscolhaCard(state = state, onEvent = onEvent)

            PingoText(
                text = "Seu banco não está aqui? Escolha Outra para lançamentos manuais.",
                style = PingoTheme.typography.caption,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }

        ContasActions(state = state, onEvent = onEvent)
    }
}

@Composable
private fun ContasActions(state: OnboardingContasUiState, onEvent: (OnboardingContasEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        if (state.salvamento == SalvamentoUiState.Error) {
            PingoText(
                text = MENSAGEM_SALVAR_CONTAS,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.expense,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        PingoButton(
            text = "Continuar",
            onClick = { onEvent(OnboardingContasEvent.Continuar) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.podeContinuar,
            loading = state.salvamento == SalvamentoUiState.Loading,
        )
    }
}
