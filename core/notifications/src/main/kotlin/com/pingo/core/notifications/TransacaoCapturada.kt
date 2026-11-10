package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.parser.TransacaoExtraida
import java.time.Instant

data class TransacaoCapturada(
    val instituicao: InstituicaoFinanceira,
    val pacote: String,
    val titulo: String?,
    val texto: String?,
    val bigText: String?,
    val postTime: Instant,
    val transacao: TransacaoExtraida,
)
