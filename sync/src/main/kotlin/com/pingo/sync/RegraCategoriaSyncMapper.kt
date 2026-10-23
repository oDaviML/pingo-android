package com.pingo.sync

import com.pingo.core.database.entity.RegraCategoriaEntity
import com.pingo.core.network.sync.RegraCategoriaChangeDto
import com.pingo.core.network.sync.RegraCategoriaSnapshotDto

internal fun RegraCategoriaEntity.toChangeDto(): RegraCategoriaChangeDto = RegraCategoriaChangeDto(
    publicId = publicId,
    categoriaPublicId = categoriaPublicId,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun RegraCategoriaSnapshotDto.toEntity(): RegraCategoriaEntity = RegraCategoriaEntity(
    publicId = publicId,
    categoriaPublicId = categoriaPublicId,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)
