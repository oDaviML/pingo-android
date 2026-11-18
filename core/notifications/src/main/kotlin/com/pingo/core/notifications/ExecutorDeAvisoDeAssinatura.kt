package com.pingo.core.notifications

import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.assinatura.JanelaDeAvisoDeAssinatura
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutorDeAvisoDeAssinatura @Inject constructor(
    private val preferences: UserPreferencesDataSource,
    private val assinaturaRepository: AssinaturaRepository,
    private val notificadorDeAssinatura: NotificadorDeAssinatura,
    private val clock: Clock,
) {
    suspend fun executar(assinaturaPublicId: String?, dataPrevistaIso: String?, alvoEpochMillis: Long): Boolean {
        val publicId = assinaturaPublicId?.takeIf { it.isNotBlank() }
        val dataEsperada = dataPrevistaIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val alvo = alvoEpochMillis.takeIf { it != Long.MIN_VALUE }?.let(Instant::ofEpochMilli)
        return if (publicId != null && dataEsperada != null && alvo != null) {
            tentarPostar(publicId, dataEsperada, alvo)
        } else {
            false
        }
    }

    private suspend fun tentarPostar(publicId: String, dataEsperada: LocalDate, alvo: Instant): Boolean {
        val ativo = preferences.avisoAssinaturasAtivo.first()
        val podeNotificar = notificadorDeAssinatura.podeNotificar()
        val dentroDaJanela = JanelaDeAvisoDeAssinatura.podePostarAvisoAgendado(clock.instant(), alvo)
        if (!ativo || !podeNotificar || !dentroDaJanela) {
            return false
        }

        val assinatura = assinaturaRepository.buscar(publicId)
        val elegivel = assinatura != null &&
            assinatura.excluidoEm == null &&
            assinatura.status == StatusAssinatura.CONFIRMADA &&
            assinatura.proximaCobrancaEstimada == dataEsperada

        return elegivel && notificadorDeAssinatura.postar(assinatura, dataEsperada)
    }
}
