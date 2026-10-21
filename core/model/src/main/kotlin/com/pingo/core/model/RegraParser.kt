package com.pingo.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class RegraParser(val id: String, val versao: Int, val padrao: String, val ativa: Boolean)
