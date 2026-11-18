package com.pingo.core.data.detalhe

import com.pingo.core.model.Lancamento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.ModoParcelamento
import java.time.Instant

enum class CampoDoDetalhe {
    VALOR,
    INSTRUMENTO,
    DESTINO,
    CATEGORIA,
    PARCELAS,
}

data class EdicaoDoLancamento(
    val publicId: String,
    val tipo: TipoLancamento,
    val valorCentavos: Long,
    val instrumentoPublicId: String?,
    val destinoPublicId: String? = null,
    val categoriaPublicId: String? = null,
    val dataHora: Instant,
    val estabelecimento: String? = null,
    val aplicarCategoriaATodos: Boolean = false,
    val totalParcelas: Int = 1,
    val modoParcelamento: ModoParcelamento = ModoParcelamento.VALOR_TOTAL,
)

sealed interface ResultadoDaEdicao {
    data class Salvo(val lancamento: Lancamento, val par: Lancamento? = null) : ResultadoDaEdicao

    data class Invalido(val campos: Set<CampoDoDetalhe>) : ResultadoDaEdicao

    data object NaoEncontrado : ResultadoDaEdicao
}
