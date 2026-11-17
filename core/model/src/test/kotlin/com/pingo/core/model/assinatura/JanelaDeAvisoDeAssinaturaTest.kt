package com.pingo.core.model.assinatura

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TimeZone

class JanelaDeAvisoDeAssinaturaTest {
    private fun instanteSaoPaulo(ano: Int, mes: Int, dia: Int, hora: Int, minuto: Int = 0, segundo: Int = 0): Instant =
        LocalDateTime.of(ano, mes, dia, hora, minuto, segundo)
            .atZone(JanelaDeAvisoDeAssinatura.ZONA_SAO_PAULO)
            .toInstant()

    @Test
    fun `alvo eh dois dias antes as 9h de Sao Paulo independente do fuso do aparelho`() {
        val fusoOriginal = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
            assertEquals(
                instanteSaoPaulo(2026, 5, 13, 9, 0),
                JanelaDeAvisoDeAssinatura.calcularAlvo(LocalDate.of(2026, 5, 15)),
            )
            assertEquals(
                instanteSaoPaulo(2026, 4, 29, 9, 0),
                JanelaDeAvisoDeAssinatura.calcularAlvo(LocalDate.of(2026, 5, 1)),
            )
            assertEquals(
                instanteSaoPaulo(2025, 12, 31, 9, 0),
                JanelaDeAvisoDeAssinatura.calcularAlvo(LocalDate.of(2026, 1, 2)),
            )
        } finally {
            TimeZone.setDefault(fusoOriginal)
        }
    }

    @Test
    fun `agendamento aceita exatamente as 9h mas bloqueia confirmacao retroativa apos 9h`() {
        val alvo = JanelaDeAvisoDeAssinatura.calcularAlvo(LocalDate.of(2026, 5, 15))

        assertTrue(JanelaDeAvisoDeAssinatura.deveAgendar(instanteSaoPaulo(2026, 5, 13, 8, 59, 59), alvo))
        assertTrue(JanelaDeAvisoDeAssinatura.deveAgendar(instanteSaoPaulo(2026, 5, 13, 9, 0, 0), alvo))
        assertFalse(JanelaDeAvisoDeAssinatura.deveAgendar(instanteSaoPaulo(2026, 5, 13, 9, 0, 1), alvo))
        assertFalse(JanelaDeAvisoDeAssinatura.deveAgendar(instanteSaoPaulo(2026, 5, 13, 9, 1, 0), alvo))
        assertFalse(JanelaDeAvisoDeAssinatura.deveAgendar(instanteSaoPaulo(2026, 5, 14, 9, 0, 0), alvo))
    }

    @Test
    fun `trabalho agendado pode postar ate 23h59 do dia alvo e nunca antes das 9h nem no dia seguinte`() {
        val alvo = JanelaDeAvisoDeAssinatura.calcularAlvo(LocalDate.of(2026, 5, 15))

        assertFalse(JanelaDeAvisoDeAssinatura.podePostarAvisoAgendado(instanteSaoPaulo(2026, 5, 13, 8, 59, 59), alvo))
        assertTrue(JanelaDeAvisoDeAssinatura.podePostarAvisoAgendado(instanteSaoPaulo(2026, 5, 13, 9, 0, 0), alvo))
        assertTrue(JanelaDeAvisoDeAssinatura.podePostarAvisoAgendado(instanteSaoPaulo(2026, 5, 13, 23, 59, 59), alvo))
        assertFalse(JanelaDeAvisoDeAssinatura.podePostarAvisoAgendado(instanteSaoPaulo(2026, 5, 14, 0, 0, 0), alvo))
    }

    @Test
    fun `nome unico e atraso inicial sao deterministicos`() {
        val alvo = JanelaDeAvisoDeAssinatura.calcularAlvo(LocalDate.of(2026, 5, 15))
        val umaHoraAntes = instanteSaoPaulo(2026, 5, 13, 8, 0, 0)

        assertEquals(3_600_000L, JanelaDeAvisoDeAssinatura.calcularAtrasoInicialMilis(umaHoraAntes, alvo))
        assertEquals(0L, JanelaDeAvisoDeAssinatura.calcularAtrasoInicialMilis(alvo.plusSeconds(10), alvo))
        assertEquals(
            "aviso-assinatura-ass-1-2026-05-15",
            JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("ass-1", LocalDate.of(2026, 5, 15)),
        )
    }
}
