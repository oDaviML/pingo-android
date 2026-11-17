package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Instrumento(
    val publicId: String,
    val contaPublicId: String,
    val tipo: TipoInstrumento,
    val apelido: String,
    val ultimosDigitos: String? = null,
    val arquivado: Boolean = false,
    val arquivadoPorCascata: Boolean = false,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val beneficio: BeneficioVoucher? = null,
    val saldoCentavos: Long? = null,
    val diaRecarga: Int? = null,
    val valorRecargaCentavos: Long? = null,
    val limiteCreditoCentavos: Long? = null,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
)
