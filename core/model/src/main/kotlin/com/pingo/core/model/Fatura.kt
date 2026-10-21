package com.pingo.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Fatura(val id: String, val instrumentoId: String, val valorCentavos: Long)
