package com.pingo.core.analytics

internal interface CanalTelemetria {
    fun atualizarColeta(habilitada: Boolean)

    fun registrarEvento(nome: String, atributos: Map<String, String>)

    fun registrarExcecao(erro: Throwable)
}
