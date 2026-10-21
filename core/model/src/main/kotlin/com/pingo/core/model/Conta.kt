package com.pingo.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Conta(val id: String, val nome: String, val saldoCentavos: Long)
