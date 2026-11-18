package com.pingo.sync

import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.network.sync.ConfiguracaoCartaoChangeDto
import com.pingo.core.network.sync.ConfiguracaoCartaoSnapshotDto

internal fun ConfiguracaoCartaoEntity.toChangeDto(): ConfiguracaoCartaoChangeDto = ConfiguracaoCartaoChangeDto(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    vigenciaInicio = vigenciaInicio.toApiValue(),
    fechamentoDia = fechamentoDia,
    vencimentoDia = vencimentoDia,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun ConfiguracaoCartaoSnapshotDto.toEntity(): ConfiguracaoCartaoEntity = ConfiguracaoCartaoEntity(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    vigenciaInicio = vigenciaInicio.toInstantValue(),
    fechamentoDia = fechamentoDia,
    vencimentoDia = vencimentoDia,
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)
