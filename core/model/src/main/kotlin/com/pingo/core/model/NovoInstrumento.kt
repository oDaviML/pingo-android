package com.pingo.core.model

data class NovoInstrumento(
    val tipo: TipoInstrumento,
    val apelido: String,
    val ultimosDigitos: String? = null,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val beneficio: BeneficioVoucher? = null,
    val saldoCentavos: Long? = null,
    val diaRecarga: Int? = null,
    val valorRecargaCentavos: Long? = null,
) {
    fun valido(): Boolean = apelidoValido(apelido) && configuracao().valida()

    fun configuracao(): ConfiguracaoDeInstrumento = ConfiguracaoDeInstrumento(
        tipo = tipo,
        ultimosDigitos = ultimosDigitos,
        fechamentoDia = fechamentoDia,
        vencimentoDia = vencimentoDia,
        beneficio = beneficio,
        saldoCentavos = saldoCentavos,
        diaRecarga = diaRecarga,
        valorRecargaCentavos = valorRecargaCentavos,
    )
}
