package com.pingo.sync

import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.network.sync.CampoPresenca
import com.pingo.core.network.sync.InstrumentoChangeDto
import com.pingo.core.network.sync.InstrumentoSnapshotDto

internal fun InstrumentoEntity.toChangeDto(): InstrumentoChangeDto = InstrumentoChangeDto(
    publicId = publicId,
    contaPublicId = contaPublicId,
    tipo = tipo.toApiValue(),
    apelido = apelido,
    ultimosDigitos = ultimosDigitos,
    arquivado = arquivado,
    fechamentoDia = fechamentoDia,
    vencimentoDia = vencimentoDia,
    beneficio = beneficio?.toApiValue(),
    saldo = saldoCentavos?.toApiReais(),
    diaRecarga = diaRecarga,
    valorRecarga = valorRecargaCentavos?.toApiReais(),
    limiteCredito = limiteCreditoCentavos?.toApiReais(),
    arquivadoPorCascata = arquivadoPorCascata,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun InstrumentoSnapshotDto.toEntity(local: InstrumentoEntity? = null): InstrumentoEntity = InstrumentoEntity(
    publicId = publicId,
    contaPublicId = contaPublicId,
    tipo = tipoInstrumentoFromApi(tipo),
    apelido = apelido,
    ultimosDigitos = ultimosDigitos.resolver { local?.ultimosDigitos },
    arquivado = arquivado,
    fechamentoDia = fechamentoDia.resolver { local?.fechamentoDia },
    vencimentoDia = vencimentoDia.resolver { local?.vencimentoDia },
    beneficio = beneficio.resolver { local?.beneficio?.toApiValue() }?.let(::beneficioFromApi),
    saldoCentavos = saldo.resolver { local?.saldoCentavos?.toApiReais() }?.toCentavos(),
    diaRecarga = diaRecarga.resolver { local?.diaRecarga },
    valorRecargaCentavos = valorRecarga.resolver { local?.valorRecargaCentavos?.toApiReais() }?.toCentavos(),
    limiteCreditoCentavos = limiteCredito.resolver { local?.limiteCreditoCentavos?.toApiReais() }?.toCentavos(),
    arquivadoPorCascata = arquivadoPorCascata.resolverProcedencia(arquivado = arquivado, local = local),
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)

private fun CampoPresenca<Boolean>.resolverProcedencia(arquivado: Boolean, local: InstrumentoEntity?): Boolean {
    if (!arquivado) {
        return false
    }

    return resolver { local?.takeIf { it.arquivado }?.arquivadoPorCascata ?: false } ?: false
}
