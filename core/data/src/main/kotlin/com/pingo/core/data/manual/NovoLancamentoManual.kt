package com.pingo.core.data.manual

import com.pingo.core.model.Lancamento
import com.pingo.core.model.TipoLancamento
import java.time.Instant

enum class CampoDeLancamentoManual {
    TIPO,
    VALOR,
    INSTRUMENTO,
    ORIGEM,
    DESTINO,
}

data class NovoLancamentoManual(
    val tipo: TipoLancamento,
    val valorCentavos: Long,
    val instrumentoPublicId: String?,
    val destinoPublicId: String? = null,
    val categoriaPublicId: String? = null,
    val dataHora: Instant,
    val estabelecimento: String? = null,
)

sealed interface ResultadoLancamentoManual {
    data class Salvo(val lancamentos: List<Lancamento>) : ResultadoLancamentoManual

    data class Invalido(val campos: Set<CampoDeLancamentoManual>) : ResultadoLancamentoManual
}
