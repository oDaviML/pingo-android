package com.pingo.core.notifications

import com.pingo.core.parser.NotificacaoBruta
import java.time.Instant

data class NotificacaoRecebida(
    val pacote: String,
    val tag: String?,
    val titulo: String?,
    val texto: String?,
    val bigText: String?,
    val postTime: Instant,
) {
    val bruta: NotificacaoBruta
        get() = NotificacaoBruta(titulo = titulo, texto = texto, bigText = bigText)
}
