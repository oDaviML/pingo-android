package com.pingo.sync

import com.pingo.core.database.entity.InstrumentoEntity
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

internal fun InstrumentoSnapshotDto.toEntity(): InstrumentoEntity = InstrumentoEntity(
    publicId = publicId,
    contaPublicId = contaPublicId,
    tipo = tipoInstrumentoFromApi(tipo),
    apelido = apelido,
    ultimosDigitos = ultimosDigitos,
    arquivado = arquivado,
    fechamentoDia = fechamentoDia,
    vencimentoDia = vencimentoDia,
    beneficio = beneficio?.let(::beneficioFromApi),
    saldoCentavos = saldo?.toCentavos(),
    diaRecarga = diaRecarga,
    valorRecargaCentavos = valorRecarga?.toCentavos(),
    limiteCreditoCentavos = limiteCredito?.toCentavos(),
    arquivadoPorCascata = arquivadoPorCascata ?: false,
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)
