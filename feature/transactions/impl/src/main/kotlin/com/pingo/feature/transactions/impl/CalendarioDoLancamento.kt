package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun CalendarioDoLancamento(data: LocalDate, hoje: LocalDate, onSelecionar: (LocalDate) -> Unit, onFechar: () -> Unit) {
    var mesVisivel by remember { mutableStateOf(YearMonth.from(data)) }
    var selecionada by remember { mutableStateOf(data) }
    val interacao = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PingoTheme.colors.scrim)
                .clickable(interactionSource = interacao, indication = null) { onFechar() },
            contentAlignment = Alignment.Center,
        ) {
            CorpoDoCalendario(
                mes = mesVisivel,
                selecionada = selecionada,
                hoje = hoje,
                gestos = GestosDoCalendario(
                    onMesAnterior = { mesVisivel = mesVisivel.minusMonths(1) },
                    onMesSeguinte = { mesVisivel = mesVisivel.plusMonths(1) },
                    onSelecionarDia = { selecionada = it },
                    onConfirmar = { onSelecionar(selecionada) },
                    onFechar = onFechar,
                ),
            )
        }
    }
}

@Composable
private fun CorpoDoCalendario(mes: YearMonth, selecionada: LocalDate, hoje: LocalDate, gestos: GestosDoCalendario) {
    val interacao = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .padding(horizontal = PingoSpacing.xxxl)
            .fillMaxWidth()
            .background(color = PingoTheme.colors.surface, shape = RoundedCornerShape(PingoRadius.xl))
            .clickable(interactionSource = interacao, indication = null) {}
            .padding(PingoSpacing.xxl)
            .testTag("calendario-do-lancamento"),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        PingoText(
            text = "DATA DO LANÇAMENTO",
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = RotulosDoNovoLancamento.dataDoCalendario(selecionada),
            style = PingoTheme.typography.headline,
        )
        CabecalhoDoMes(mes = mes, onAnterior = gestos.onMesAnterior, onSeguinte = gestos.onMesSeguinte)
        DiasDaSemana()
        GradeDeDias(mes = mes, selecionada = selecionada, hoje = hoje, onSelecionar = gestos.onSelecionarDia)
        AcoesDoCalendario(onCancelar = gestos.onFechar, onConfirmar = gestos.onConfirmar)
    }
}

private data class GestosDoCalendario(
    val onMesAnterior: () -> Unit,
    val onMesSeguinte: () -> Unit,
    val onSelecionarDia: (LocalDate) -> Unit,
    val onConfirmar: () -> Unit,
    val onFechar: () -> Unit,
)

@Composable
private fun CabecalhoDoMes(mes: YearMonth, onAnterior: () -> Unit, onSeguinte: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavegadorDeMes(icone = PingoIcons.ArrowLeft, descricao = "Mês anterior", testTag = "mes-anterior", onClick = onAnterior)
        PingoText(text = RotulosDoNovoLancamento.mes(mes), style = PingoTheme.typography.subtitle)
        NavegadorDeMes(icone = PingoIcons.ChevronRight, descricao = "Próximo mês", testTag = "mes-seguinte", onClick = onSeguinte)
    }
}

@Composable
private fun NavegadorDeMes(icone: ImageVector, descricao: String, testTag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = icone,
            contentDescription = descricao,
            modifier = Modifier.size(20.dp),
            tint = PingoTheme.colors.onBackground,
        )
    }
}

@Composable
private fun DiasDaSemana() {
    Row(modifier = Modifier.fillMaxWidth()) {
        RotulosDoNovoLancamento.diasDaSemana.forEach { dia ->
            PingoText(
                text = dia,
                style = PingoTheme.typography.caption,
                color = PingoTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GradeDeDias(mes: YearMonth, selecionada: LocalDate, hoje: LocalDate, onSelecionar: (LocalDate) -> Unit) {
    val celulas = celulasDoMes(mes)

    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
        celulas.chunked(DIAS_NA_SEMANA).forEach { semana ->
            Row(modifier = Modifier.fillMaxWidth()) {
                semana.forEach { data ->
                    CelulaDeDia(
                        data = data,
                        selecionada = selecionada,
                        hoje = hoje,
                        onSelecionar = onSelecionar,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CelulaDeDia(
    data: LocalDate?,
    selecionada: LocalDate,
    hoje: LocalDate,
    onSelecionar: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(40.dp), contentAlignment = Alignment.Center) {
        if (data != null) {
            DiaDoCalendario(data = data, selecionada = selecionada, hoje = hoje, onSelecionar = onSelecionar)
        }
    }
}

@Composable
private fun DiaDoCalendario(data: LocalDate, selecionada: LocalDate, hoje: LocalDate, onSelecionar: (LocalDate) -> Unit) {
    val selecionado = data == selecionada
    val futuro = data.isAfter(hoje)
    val cor = when {
        selecionado -> PingoTheme.colors.onPrimary
        futuro -> PingoTheme.colors.onDisabled
        else -> PingoTheme.colors.onBackground
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (selecionado) PingoTheme.colors.primary else Color.Transparent,
                shape = RoundedCornerShape(PingoRadius.full),
            )
            .then(
                if (data == hoje &&
                    !selecionado
                ) {
                    Modifier.border(1.5.dp, PingoTheme.colors.primary, RoundedCornerShape(PingoRadius.full))
                } else {
                    Modifier
                },
            )
            .testTag("dia-$data")
            .clickable(enabled = !futuro, onClick = { onSelecionar(data) }),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = data.dayOfMonth.toString(),
            style = PingoTheme.typography.body,
            color = cor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AcoesDoCalendario(onCancelar: () -> Unit, onConfirmar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoButton(text = "Cancelar", onClick = onCancelar, type = PingoButtonType.Text)
        PingoButton(
            text = "Confirmar",
            onClick = onConfirmar,
            modifier = Modifier.testTag("confirmar-data"),
        )
    }
}

private fun celulasDoMes(mes: YearMonth): List<LocalDate?> {
    val deslocamento = mes.atDay(1).dayOfWeek.value % DIAS_NA_SEMANA
    val dias = (1..mes.lengthOfMonth()).map { mes.atDay(it) }

    return List(deslocamento) { null } + dias
}

private const val DIAS_NA_SEMANA = 7
