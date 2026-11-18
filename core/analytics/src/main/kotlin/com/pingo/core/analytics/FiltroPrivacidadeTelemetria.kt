package com.pingo.core.analytics

internal object FiltroPrivacidadeTelemetria {
    private val EVENTOS_PERMITIDOS = setOf(
        "captura_processada",
        "sincronizacao_concluida",
        "atualizacao_verificada",
        "regras_atualizadas",
    )

    private val ATRIBUTOS_PERMITIDOS = setOf(
        "instituicao",
        "resultado",
        "reconhecidas",
        "revisao",
        "ignoradas",
        "nao_entendidas",
        "origem",
        "versao_regras",
        "versao_app",
        "status",
    )

    private val TERMOS_SENSIVEIS = listOf(
        "texto",
        "textobruto",
        "corpo",
        "titulo",
        "mensagem",
        "estabelecimento",
        "descricao",
        "notificacao",
        "conteudo",
    )

    fun filtrarEvento(nome: String): String? {
        val limpo = nome.trim()
        return limpo.takeIf { it in EVENTOS_PERMITIDOS }
    }

    fun filtrarAtributos(atributos: Map<String, String>): Map<String, String> = buildMap {
        atributos.forEach { (chaveRaw, valorRaw) ->
            val chave = chaveRaw.trim()
            if (chave in ATRIBUTOS_PERMITIDOS && !contemChaveSensivel(chave)) {
                val valorLimpo = sanitizarValor(valorRaw)
                if (valorLimpo.isNotEmpty()) {
                    put(chave, valorLimpo)
                }
            }
        }
    }

    fun sanitizarExcecao(erro: Throwable): Throwable =
        ExcecaoSanitizadaTelemetria(erro::class.java.simpleName.ifBlank { "ErroDesconhecido" })

    private fun contemChaveSensivel(chave: String): Boolean {
        val normalizada = chave.lowercase()
        return TERMOS_SENSIVEIS.any { termo -> normalizada.contains(termo) }
    }

    private fun sanitizarValor(valor: String): String = valor
        .replace('\n', ' ')
        .replace('\r', ' ')
        .trim()
        .take(TAMANHO_MAXIMO_VALOR)

    private class ExcecaoSanitizadaTelemetria(tipoOriginal: String) :
        RuntimeException("Falha tecnica sanitizada: $tipoOriginal", null, false, false)

    private const val TAMANHO_MAXIMO_VALOR = 64
}
