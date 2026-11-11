package com.pingo.core.notifications

data class ConteudoDaRevisao(val titulo: String, val resumo: String, val aviso: String?) {
    val texto: String
        get() = aviso ?: resumo

    val textoLongo: String
        get() = aviso?.let { "$it\n$resumo" } ?: resumo
}
