package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

private sealed interface TeclaDoTeclado {
    val tag: String

    data class Numero(val digito: Int) : TeclaDoTeclado {
        override val tag: String = "tecla-$digito"
    }

    data object Virgula : TeclaDoTeclado {
        override val tag: String = "tecla-virgula"
    }

    data object Apagar : TeclaDoTeclado {
        override val tag: String = "tecla-apagar"
    }
}

private val LINHAS_DO_TECLADO: List<List<TeclaDoTeclado>> = listOf(
    listOf(TeclaDoTeclado.Numero(1), TeclaDoTeclado.Numero(2), TeclaDoTeclado.Numero(3)),
    listOf(TeclaDoTeclado.Numero(4), TeclaDoTeclado.Numero(5), TeclaDoTeclado.Numero(6)),
    listOf(TeclaDoTeclado.Numero(7), TeclaDoTeclado.Numero(8), TeclaDoTeclado.Numero(9)),
    listOf(TeclaDoTeclado.Virgula, TeclaDoTeclado.Numero(0), TeclaDoTeclado.Apagar),
)

@Composable
internal fun TecladoNumerico(habilitado: Boolean, onDigito: (Int) -> Unit, onApagar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PingoTheme.colors.surfaceVariant,
                shape = RoundedCornerShape(topStart = PingoRadius.xl, topEnd = PingoRadius.xl),
            )
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md)
            .testTag("teclado-numerico"),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        LINHAS_DO_TECLADO.forEach { linha ->
            LinhaDoTeclado(teclas = linha, habilitado = habilitado, onDigito = onDigito, onApagar = onApagar)
        }
    }
}

@Composable
private fun LinhaDoTeclado(teclas: List<TeclaDoTeclado>, habilitado: Boolean, onDigito: (Int) -> Unit, onApagar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        teclas.forEach { tecla ->
            TeclaNumerica(
                tecla = tecla,
                habilitado = habilitado,
                onDigito = onDigito,
                onApagar = onApagar,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeclaNumerica(
    tecla: TeclaDoTeclado,
    habilitado: Boolean,
    onDigito: (Int) -> Unit,
    onApagar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ativa = habilitado && tecla != TeclaDoTeclado.Virgula
    val forma = RoundedCornerShape(PingoRadius.md)

    Box(
        modifier = modifier
            .height(48.dp)
            .background(color = PingoTheme.colors.surface, shape = forma)
            .testTag(tecla.tag)
            .clickable(
                enabled = ativa,
                role = Role.Button,
                onClick = { acionarTecla(tecla, onDigito, onApagar) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        ConteudoDaTecla(tecla = tecla, ativa = habilitado)
    }
}

@Composable
private fun ConteudoDaTecla(tecla: TeclaDoTeclado, ativa: Boolean) {
    val cor = if (ativa) PingoTheme.colors.onBackground else PingoTheme.colors.onSurfaceVariant

    when (tecla) {
        is TeclaDoTeclado.Numero -> PingoText(
            text = tecla.digito.toString(),
            style = PingoTheme.typography.subtitle,
            color = cor,
        )

        TeclaDoTeclado.Virgula -> PingoText(
            text = ",",
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onSurfaceVariant,
        )

        TeclaDoTeclado.Apagar -> PingoIcon(
            imageVector = PingoIcons.Backspace,
            contentDescription = "Apagar",
            modifier = Modifier.size(22.dp),
            tint = cor,
        )
    }
}

private fun acionarTecla(tecla: TeclaDoTeclado, onDigito: (Int) -> Unit, onApagar: () -> Unit) {
    when (tecla) {
        is TeclaDoTeclado.Numero -> onDigito(tecla.digito)
        TeclaDoTeclado.Apagar -> onApagar()
        TeclaDoTeclado.Virgula -> Unit
    }
}
