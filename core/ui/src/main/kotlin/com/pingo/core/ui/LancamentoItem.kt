package com.pingo.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pingo.core.model.Lancamento

@Composable
fun LancamentoItem(lancamento: Lancamento) {
    Text(text = lancamento.descricao)
}
