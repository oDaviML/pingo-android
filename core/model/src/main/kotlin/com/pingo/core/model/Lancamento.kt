package com.pingo.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Lancamento(val id: String, val descricao: String, val valorCentavos: Long)
