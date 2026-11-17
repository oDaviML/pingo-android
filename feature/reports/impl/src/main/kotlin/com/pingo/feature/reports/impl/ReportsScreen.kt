package com.pingo.feature.reports.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun ReportsScreen(onSelecionarAba: (AbaInferior) -> Unit, onOpenNovo: (() -> Unit)? = null, mostrarBarraInferior: Boolean = true) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PingoText(text = "Relatórios em breve")
        }
        if (mostrarBarraInferior) {
            PingoBottomBar(
                selecionada = AbaInferior.RELATORIOS,
                onSelecionar = onSelecionarAba,
                onNovo = onOpenNovo,
            )
        }
    }
}
