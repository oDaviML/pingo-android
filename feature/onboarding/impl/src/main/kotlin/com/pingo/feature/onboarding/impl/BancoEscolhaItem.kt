package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoCheckboxMark
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun BancosEscolhaCard(state: OnboardingContasUiState, onEvent: (OnboardingContasEvent) -> Unit) {
    PingoCard(contentPadding = 0.dp, gap = 0.dp) {
        BancoOferecido.todos.forEachIndexed { indice, banco ->
            if (indice > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PingoTheme.colors.outlineVariant),
                )
            }

            BancoEscolhaItem(
                banco = banco,
                escolha = state.escolhas.firstOrNull { it.instituicao == banco.instituicao },
                onEvent = onEvent,
            )
        }
    }
}

@Composable
internal fun BancoEscolhaItem(banco: BancoOferecido, escolha: EscolhaBanco?, onEvent: (OnboardingContasEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        BancoCabecalho(
            banco = banco,
            selecionado = escolha != null,
            onAlternar = { onEvent(OnboardingContasEvent.AlternarBanco(banco.instituicao)) },
        )

        if (escolha != null) {
            ContaEscolhaCampos(escolha = escolha, onEvent = onEvent)
        }
    }
}

@Composable
private fun BancoCabecalho(banco: BancoOferecido, selecionado: Boolean, onAlternar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .toggleable(value = selecionado, role = Role.Checkbox, onValueChange = { onAlternar() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        BancoLetras(letras = banco.letras)

        Column(modifier = Modifier.weight(1f)) {
            PingoText(
                text = banco.nome,
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
            )
            PingoText(
                text = banco.descricao,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }

        PingoCheckboxMark(checked = selecionado)
    }
}

@Composable
private fun BancoLetras(letras: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = PingoTheme.colors.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = letras,
            style = PingoTheme.typography.label,
            color = PingoTheme.colors.onPrimaryContainer,
        )
    }
}
