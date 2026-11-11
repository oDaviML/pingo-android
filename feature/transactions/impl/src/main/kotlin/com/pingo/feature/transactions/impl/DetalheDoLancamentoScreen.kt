package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun DetalheDoLancamentoScreen(publicId: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("detalhe-$publicId")
            .background(PingoTheme.colors.background),
    ) {
        PingoTopBar(title = "Detalhe do lançamento", onBack = onBack)
        PingoText(
            text = "Editar, excluir e ver a origem do lançamento.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = PingoSpacing.lg),
        )
    }
}
