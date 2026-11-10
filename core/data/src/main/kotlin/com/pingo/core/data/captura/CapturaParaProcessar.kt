package com.pingo.core.data.captura

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.parser.TransacaoExtraida
import java.time.Instant

data class CapturaParaProcessar(
    val instituicao: InstituicaoFinanceira,
    val transacao: TransacaoExtraida,
    val postTime: Instant,
    val textoBruto: String? = null,
)
