package com.pingo.core.ui

import androidx.compose.runtime.Composable
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.model.Lancamento

@Composable
fun LancamentoItem(lancamento: Lancamento) {
    PingoText(text = lancamento.estabelecimento ?: "Lançamento")
}
