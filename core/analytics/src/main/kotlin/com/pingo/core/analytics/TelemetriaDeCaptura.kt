package com.pingo.core.analytics

enum class ResultadoTelemetriaCaptura(val codigo: String) {
    RECONHECIDA("reconhecida"),
    REVISAO("revisao"),
    IGNORADA("ignorada"),
    NAO_ENTENDIDA("nao_entendida"),
}

object TelemetriaDeCaptura {
    const val EVENTO_CAPTURA_PROCESSADA = "captura_processada"

    fun registrarCaptura(analytics: AnalyticsFacade, instituicao: String, resultado: ResultadoTelemetriaCaptura) {
        analytics.logEvent(
            name = EVENTO_CAPTURA_PROCESSADA,
            attributes = mapOf(
                "instituicao" to instituicao.lowercase(),
                "resultado" to resultado.codigo,
            ),
        )
    }
}
