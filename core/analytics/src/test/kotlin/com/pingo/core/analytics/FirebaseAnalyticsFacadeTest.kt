package com.pingo.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAnalyticsFacadeTest {
    @Test
    fun `comeca desligada e ignora eventos e erros enquanto opt in estiver falso`() {
        val canal = FakeCanalTelemetria()
        val facade = FirebaseAnalyticsFacade(canal)

        assertFalse(facade.estaHabilitada())

        TelemetriaDeCaptura.registrarCaptura(
            analytics = facade,
            instituicao = "Nubank",
            resultado = ResultadoTelemetriaCaptura.RECONHECIDA,
        )
        facade.recordError(IllegalStateException("Compra R$ 42,90 Padaria"))

        assertTrue(canal.eventos.isEmpty())
        assertTrue(canal.excecoes.isEmpty())
        assertTrue(canal.alteracoesColeta.isEmpty())
    }

    @Test
    fun `com opt in ativo envia contadores permitidos e desliga imediatamente ao desativar`() {
        val canal = FakeCanalTelemetria()
        val facade = FirebaseAnalyticsFacade(canal)

        facade.setAnalyticsEnabled(true)
        assertTrue(facade.estaHabilitada())

        TelemetriaDeCaptura.registrarCaptura(
            analytics = facade,
            instituicao = "NUBANK",
            resultado = ResultadoTelemetriaCaptura.REVISAO,
        )

        assertEquals(1, canal.eventos.size)
        val (nome, atributos) = canal.eventos.single()
        assertEquals(TelemetriaDeCaptura.EVENTO_CAPTURA_PROCESSADA, nome)
        assertEquals(mapOf("instituicao" to "nubank", "resultado" to "revisao"), atributos)

        facade.setAnalyticsEnabled(false)
        assertFalse(facade.estaHabilitada())

        TelemetriaDeCaptura.registrarCaptura(
            analytics = facade,
            instituicao = "ITAU",
            resultado = ResultadoTelemetriaCaptura.IGNORADA,
        )
        facade.recordError(IllegalArgumentException("Falha apos desligar"))

        assertEquals(1, canal.eventos.size)
        assertTrue(canal.excecoes.isEmpty())
        assertEquals(listOf(true, false), canal.alteracoesColeta)
    }

    @Test
    fun `rnf11 bloqueia texto bruto atributos nao permitidos e sanitiza mensagem de excecao`() {
        val canal = FakeCanalTelemetria()
        val facade = FirebaseAnalyticsFacade(canal)
        facade.setAnalyticsEnabled(true)

        facade.logEvent(
            name = "captura_processada",
            attributes = mapOf(
                "instituicao" to "caju",
                "resultado" to "nao_entendida",
                "textoBruto" to "Caju VR compra de R$ 35,00 na Padaria do Ze",
                "texto" to "conteudo sensivel",
                "estabelecimento" to "Padaria do Ze",
                "mensagem" to "notificacao secreta",
            ),
        )
        facade.logEvent(
            name = "evento_nao_permitido",
            attributes = mapOf("instituicao" to "inter"),
        )
        facade.recordError(IllegalStateException("Compra de R$ 199,90 no Mercado Livre"))

        assertEquals(1, canal.eventos.size)
        assertEquals(
            mapOf("instituicao" to "caju", "resultado" to "nao_entendida"),
            canal.eventos.single().second,
        )
        assertEquals(1, canal.excecoes.size)
        val erroRegistrado = canal.excecoes.single().message.orEmpty()
        assertTrue(erroRegistrado.contains("IllegalStateException"))
        assertFalse(erroRegistrado.contains("Mercado Livre"))
        assertFalse(erroRegistrado.contains("199,90"))
    }

    private class FakeCanalTelemetria : CanalTelemetria {
        val alteracoesColeta = mutableListOf<Boolean>()
        val eventos = mutableListOf<Pair<String, Map<String, String>>>()
        val excecoes = mutableListOf<Throwable>()

        override fun atualizarColeta(habilitada: Boolean) {
            alteracoesColeta += habilitada
        }

        override fun registrarEvento(nome: String, atributos: Map<String, String>) {
            eventos += nome to atributos
        }

        override fun registrarExcecao(erro: Throwable) {
            excecoes += erro
        }
    }
}
