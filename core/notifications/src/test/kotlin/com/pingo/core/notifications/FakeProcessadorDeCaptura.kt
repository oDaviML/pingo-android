package com.pingo.core.notifications

import com.pingo.core.data.captura.CapturaParaProcessar
import com.pingo.core.data.captura.ProcessadorDeCaptura
import com.pingo.core.data.captura.ResultadoProcessamento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento

internal class FakeProcessadorDeCaptura : ProcessadorDeCaptura {
    var resultado: ResultadoProcessamento? = null
    var status: StatusLancamento = StatusLancamento.CONFIRMADO
    val capturas: MutableList<CapturaParaProcessar> = mutableListOf()

    override suspend fun processar(captura: CapturaParaProcessar): ResultadoProcessamento {
        capturas += captura

        return resultado ?: ResultadoProcessamento.Gravado(lancamentoDe(captura, status))
    }

    companion object {
        fun lancamentoDe(captura: CapturaParaProcessar, status: StatusLancamento = StatusLancamento.CONFIRMADO): Lancamento = Lancamento(
            publicId = "lancamento-${captura.postTime.toEpochMilli()}",
            instrumentoPublicId = "instrumento-1",
            valorCentavos = captura.transacao.valorCentavos,
            dataHora = captura.postTime,
            tipo = TipoLancamento.DESPESA,
            estabelecimento = captura.transacao.estabelecimento,
            status = status,
            origem = OrigemLancamento.NOTIFICACAO,
            confianca = 1.0,
            criadoEm = captura.postTime,
            atualizadoEm = captura.postTime,
        )
    }
}
