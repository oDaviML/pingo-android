package com.pingo.core.model.assinatura

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object JanelaDeAvisoDeAssinatura {
    val ZONA_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")
    const val DIAS_ANTECEDENCIA: Long = 2L
    const val HORA_AVISO: Int = 9
    const val MINUTO_AVISO: Int = 0

    fun calcularAlvo(proximaCobrancaEstimada: LocalDate): Instant = proximaCobrancaEstimada
        .minusDays(DIAS_ANTECEDENCIA)
        .atTime(HORA_AVISO, MINUTO_AVISO)
        .atZone(ZONA_SAO_PAULO)
        .toInstant()

    fun dataLocalEmSaoPaulo(instante: Instant): LocalDate = LocalDate.ofInstant(instante, ZONA_SAO_PAULO)

    fun deveAgendar(agora: Instant, alvo: Instant): Boolean = !agora.isAfter(alvo)

    fun podePostarAvisoAgendado(agora: Instant, alvo: Instant): Boolean =
        !agora.isBefore(alvo) && dataLocalEmSaoPaulo(agora) == dataLocalEmSaoPaulo(alvo)

    fun calcularAtrasoInicialMilis(agora: Instant, alvo: Instant): Long = maxOf(0L, Duration.between(agora, alvo).toMillis())

    fun nomeTrabalhoUnico(assinaturaPublicId: String, proximaCobrancaEstimada: LocalDate): String =
        "aviso-assinatura-$assinaturaPublicId-$proximaCobrancaEstimada"
}
