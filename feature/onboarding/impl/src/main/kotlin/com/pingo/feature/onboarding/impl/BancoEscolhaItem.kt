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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoCheckboxMark
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.AvatarDaInstituicao
import com.pingo.core.ui.contas.ContaFormulario
import com.pingo.core.ui.contas.ContaFormularioCampos

@Composable
internal fun BancosEscolhaCard(state: OnboardingContasUiState, onEvent: (OnboardingContasEvent) -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
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
                conta = state.contas.firstOrNull { it.instituicao == banco.instituicao },
                habilitado = state.formularioEditavel,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
internal fun BancoEscolhaItem(
    banco: BancoOferecido,
    conta: ContaFormulario?,
    habilitado: Boolean,
    onEvent: (OnboardingContasEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        BancoCabecalho(
            banco = banco,
            selecionado = conta != null,
            habilitado = habilitado,
            onAlternar = { onEvent(OnboardingContasEvent.AlternarBanco(banco.instituicao)) },
        )

        if (conta != null) {
            ContaFormularioCampos(
                formulario = conta,
                habilitado = habilitado,
                onEvento = { evento ->
                    onEvent(OnboardingContasEvent.AlterarFormulario(instituicao = banco.instituicao, evento = evento))
                },
            )
        }
    }
}

@Composable
private fun BancoCabecalho(banco: BancoOferecido, selecionado: Boolean, habilitado: Boolean, onAlternar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .toggleable(value = selecionado, enabled = habilitado, role = Role.Checkbox, onValueChange = { onAlternar() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        AvatarDaInstituicao(instituicao = banco.instituicao, size = 40.dp)

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
