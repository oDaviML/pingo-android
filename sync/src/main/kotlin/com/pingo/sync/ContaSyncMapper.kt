package com.pingo.sync

import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.network.sync.ContaChangeDto
import com.pingo.core.network.sync.ContaSnapshotDto

internal fun ContaEntity.toChangeDto(): ContaChangeDto = ContaChangeDto(
    publicId = publicId,
    instituicao = instituicao.toApiValue(),
    apelido = apelido,
    tipo = tipo.toApiValue(),
    arquivada = arquivada,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun ContaSnapshotDto.toEntity(): ContaEntity = ContaEntity(
    publicId = publicId,
    instituicao = instituicaoFromApi(instituicao),
    apelido = apelido,
    tipo = tipoContaFromApi(tipo),
    arquivada = arquivada,
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)
