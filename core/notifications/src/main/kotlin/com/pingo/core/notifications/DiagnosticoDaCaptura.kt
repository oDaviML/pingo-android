package com.pingo.core.notifications

import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import java.time.Duration
import java.time.Instant

enum class SituacaoDaCaptura {
    ATIVA,
    PARADA,
    SEM_LEITURA,
}

data class DiagnosticoDaCaptura(
    val situacao: SituacaoDaCaptura,
    val motivo: MotivoParada? = null,
    val ultimaLeituraEm: Instant? = null,
    val ultimaLeituraInstituicao: InstituicaoFinanceira? = null,
    val semLeituraDesde: Instant? = null,
) {
    val parada: Boolean
        get() = situacao != SituacaoDaCaptura.ATIVA
}

object AvaliadorDaCaptura {
    val SILENCIO_PARA_ALERTA: Duration = Duration.ofHours(48)

    fun avaliar(
        estado: EstadoListenerPersistido,
        leituras: Map<InstituicaoFinanceira, Instant>,
        observandoDesde: Instant?,
        agora: Instant,
    ): DiagnosticoDaCaptura {
        val ultimaLeitura = leituras.maxByOrNull { (_, em) -> em }
        val ultimaLeituraEm = ultimaLeitura?.value
        val ultimaLeituraInstituicao = ultimaLeitura?.key

        if (estado.estado == EstadoListener.PARADO) {
            return DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.PARADA,
                motivo = estado.motivo,
                ultimaLeituraEm = ultimaLeituraEm,
                ultimaLeituraInstituicao = ultimaLeituraInstituicao,
            )
        }

        val referencia = listOfNotNull(observandoDesde, ultimaLeituraEm).maxOrNull()
        val semLeitura = referencia != null && Duration.between(referencia, agora) >= SILENCIO_PARA_ALERTA

        return DiagnosticoDaCaptura(
            situacao = if (semLeitura) SituacaoDaCaptura.SEM_LEITURA else SituacaoDaCaptura.ATIVA,
            ultimaLeituraEm = ultimaLeituraEm,
            ultimaLeituraInstituicao = ultimaLeituraInstituicao,
            semLeituraDesde = referencia.takeIf { semLeitura },
        )
    }
}
