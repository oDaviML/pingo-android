package com.pingo.feature.capturestatus.impl

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TempoRelativo {
    private val HORA = DateTimeFormatter.ofPattern("HH:mm")

    fun formatar(em: Instant, agora: Instant, zona: ZoneId): String {
        val duracao = Duration.between(em, agora)
        val data = em.atZone(zona).toLocalDate()
        val hoje = agora.atZone(zona).toLocalDate()

        return when {
            duracao.isNegative || duracao < Duration.ofMinutes(1) -> "agora"
            duracao < Duration.ofHours(1) -> "há ${duracao.toMinutes()} min"
            data == hoje -> "há ${duracao.toHours()} h"
            data == hoje.minusDays(1) -> "ontem, ${HORA.format(em.atZone(zona))}"
            duracao.toDays() <= 1 -> "há 1 dia"
            else -> "há ${duracao.toDays()} dias"
        }
    }
}
