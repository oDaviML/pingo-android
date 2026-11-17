package com.pingo.core.data.impl

import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import java.time.Instant
import java.util.UUID

internal fun NovaConta.criar(agora: Instant): ContaCriada {
    val contaPublicId = UUID.randomUUID().toString()

    return ContaCriada(
        conta = Conta(
            publicId = contaPublicId,
            instituicao = instituicao,
            apelido = apelido.trim(),
            tipo = tipo,
            criadoEm = agora,
            atualizadoEm = agora,
        ),
        instrumentos = instrumentos.map { it.criar(contaPublicId = contaPublicId, agora = agora) },
    )
}

private fun NovoInstrumento.criar(contaPublicId: String, agora: Instant): Instrumento = Instrumento(
    publicId = UUID.randomUUID().toString(),
    contaPublicId = contaPublicId,
    tipo = tipo,
    apelido = apelido.trim(),
    ultimosDigitos = ultimosDigitos,
    fechamentoDia = fechamentoDia,
    vencimentoDia = vencimentoDia,
    beneficio = beneficio,
    saldoCentavos = saldoCentavos,
    diaRecarga = diaRecarga,
    valorRecargaCentavos = valorRecargaCentavos,
    criadoEm = agora,
    atualizadoEm = agora,
)
