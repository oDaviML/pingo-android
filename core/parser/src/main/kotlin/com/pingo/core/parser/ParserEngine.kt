package com.pingo.core.parser

interface ParserEngine {
    fun classificar(notificacao: NotificacaoBruta, regras: VersaoRegras?): ResultadoParse
}
