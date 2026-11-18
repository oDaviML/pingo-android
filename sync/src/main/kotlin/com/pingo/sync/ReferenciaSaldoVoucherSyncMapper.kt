package com.pingo.sync

import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.network.sync.ReferenciaSaldoVoucherChangeDto
import com.pingo.core.network.sync.ReferenciaSaldoVoucherSnapshotDto

internal fun ReferenciaSaldoVoucherEntity.toChangeDto(): ReferenciaSaldoVoucherChangeDto = ReferenciaSaldoVoucherChangeDto(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    tipoReferencia = tipoReferencia.toApiValue(),
    saldoCentavos = saldoCentavos,
    deltaCentavos = deltaCentavos,
    instanteReferencia = instanteReferencia.toApiValue(),
    lancamentoPublicId = lancamentoPublicId,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun ReferenciaSaldoVoucherSnapshotDto.toEntity(): ReferenciaSaldoVoucherEntity = ReferenciaSaldoVoucherEntity(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    tipoReferencia = tipoReferenciaFromApi(tipoReferencia),
    saldoCentavos = saldoCentavos,
    deltaCentavos = deltaCentavos,
    instanteReferencia = instanteReferencia.toInstantValue(),
    lancamentoPublicId = lancamentoPublicId,
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)
