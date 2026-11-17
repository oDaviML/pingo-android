package com.pingo.feature.accounts.impl

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoCheckboxMark
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.ui.contas.descricaoDeExibicao
import com.pingo.core.ui.contas.instituicoesFinanceiras
import com.pingo.core.ui.contas.letrasDeExibicao
import com.pingo.core.ui.contas.nomeDeExibicao

@Composable
internal fun InstituicaoEscolha(
    selecionada: InstituicaoFinanceira,
    onSelecionar: (InstituicaoFinanceira) -> Unit,
    habilitado: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(
            text = "Qual é a instituição?",
            style = PingoTheme.typography.title,
            color = PingoTheme.colors.onBackground,
        )
        PingoText(
            text = "Os 5 bancos suportados leem as notificações sozinhos. Outra serve para lançamentos manuais.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )

        PingoCard(contentPadding = 0.dp, gap = 0.dp) {
            instituicoesFinanceiras.forEachIndexed { indice, instituicao ->
                if (indice > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(PingoTheme.colors.outlineVariant),
                    )
                }

                InstituicaoItem(
                    instituicao = instituicao,
                    selecionada = instituicao == selecionada,
                    habilitado = habilitado,
                    onSelecionar = { onSelecionar(instituicao) },
                )
            }
        }
    }
}

@Composable
private fun InstituicaoItem(instituicao: InstituicaoFinanceira, selecionada: Boolean, habilitado: Boolean, onSelecionar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instituicao-${instituicao.name}")
            .heightIn(min = PingoLayout.minTouchTarget)
            .selectable(selected = selecionada, enabled = habilitado, role = Role.RadioButton, onClick = onSelecionar)
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = PingoTheme.colors.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            PingoText(
                text = instituicao.letrasDeExibicao(),
                style = PingoTheme.typography.label,
                color = PingoTheme.colors.onPrimaryContainer,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            PingoText(
                text = instituicao.nomeDeExibicao(),
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
            )
            PingoText(
                text = instituicao.descricaoDeExibicao(),
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }

        PingoCheckboxMark(checked = selecionada)
    }
}
