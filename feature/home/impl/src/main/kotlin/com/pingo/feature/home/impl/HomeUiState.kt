package com.pingo.feature.home.impl

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(val capturaAutomatica: Boolean = true, val semLeituraHa48h: Boolean = false) {
    val capturaParada: Boolean
        get() = !capturaAutomatica || semLeituraHa48h
}
