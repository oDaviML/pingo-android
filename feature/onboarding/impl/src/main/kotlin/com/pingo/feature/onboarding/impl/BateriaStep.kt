package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.FabricanteDoSistema

@Composable
internal fun BateriaBody(state: OnboardingPermissoesUiState, onEvent: (OnboardingPermissoesEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg)) {
        PingoText(
            text = "Mantenha a captura ligada",
            style = PingoTheme.typography.headline,
        )
        PingoText(
            text = "Alguns celulares fecham apps em segundo plano. Escolha o seu para ver o ajuste certo.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )

        FabricantesEscolha(
            selecionado = state.fabricanteSelecionado,
            onSelecionar = { fabricante -> onEvent(OnboardingPermissoesEvent.SelecionarFabricante(fabricante)) },
        )

        InstrucoesCard(state = state)

        PermissaoItem(
            titulo = "Sem restrição de bateria",
            descricao = if (state.semRestricaoDeBateria) {
                "Tudo certo, o Android não vai parar a leitura."
            } else {
                "Evita que o Android pare a leitura"
            },
            concedida = state.semRestricaoDeBateria,
            acao = "Corrigir",
            onAcao = { onEvent(OnboardingPermissoesEvent.AbrirAjustesDeBateria) },
        )

        PermissaoItem(
            titulo = "Início automático",
            descricao = "Retoma a captura ao ligar o celular",
            concedida = false,
            acao = "Corrigir",
            onAcao = { onEvent(OnboardingPermissoesEvent.AbrirInicioAutomatico) },
        )
    }
}

@Composable
private fun InstrucoesCard(state: OnboardingPermissoesUiState) {
    PingoCard(
        modifier = Modifier.pingoElevation(
            level = PingoElevationLevel.Level1,
            shape = RoundedCornerShape(PingoRadius.xl),
        ),
    ) {
        PingoText(
            text = state.instrucoes.titulo.uppercase(),
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PassosNumerados(passos = state.instrucoes.passos)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FabricantesEscolha(selecionado: FabricanteDoSistema, onSelecionar: (FabricanteDoSistema) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        FabricanteDoSistema.entries.forEach { fabricante ->
            PingoChoiceChip(
                text = fabricante.rotulo,
                selected = selecionado == fabricante,
                onClick = { onSelecionar(fabricante) },
            )
        }
    }
}

private val FabricanteDoSistema.rotulo: String
    get() = when (this) {
        FabricanteDoSistema.SAMSUNG -> "Samsung"
        FabricanteDoSistema.XIAOMI -> "Xiaomi"
        FabricanteDoSistema.MOTOROLA -> "Motorola"
        FabricanteDoSistema.OUTRO -> "Outro"
    }
