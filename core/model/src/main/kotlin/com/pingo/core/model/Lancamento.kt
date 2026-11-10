package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Lancamento(
    val publicId: String,
    val instrumentoPublicId: String,
    val categoriaPublicId: String? = null,
    val faturaPublicId: String? = null,
    val assinaturaPublicId: String? = null,
    val compraPaiPublicId: String? = null,
    val estornoDePublicId: String? = null,
    val transferenciaId: String? = null,
    val valorCentavos: Long,
    val dataHora: Instant,
    val tipo: TipoLancamento,
    val estabelecimento: String? = null,
    val status: StatusLancamento,
    val origem: OrigemLancamento,
    val confianca: Double,
    val motivoPendencia: String? = null,
    val hashDuplicata: String? = null,
    val observacao: String? = null,
    val parcelaNumero: Int = 1,
    val totalParcelas: Int = 1,
    val valorTotalCompraCentavos: Long? = null,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
    val textoBruto: String? = null,
)
