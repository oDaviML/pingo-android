package com.pingo.sync

import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.network.sync.CategoriaChangeDto
import com.pingo.core.network.sync.CategoriaSnapshotDto

internal fun CategoriaEntity.toChangeDto(): CategoriaChangeDto = CategoriaChangeDto(
    publicId = publicId,
    nome = nome,
    tipo = tipo.toApiValue(),
    padrao = padrao,
    arquivada = arquivada,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun CategoriaSnapshotDto.toEntity(): CategoriaEntity = CategoriaEntity(
    publicId = publicId,
    nome = nome,
    tipo = tipoCategoriaFromApi(tipo),
    padrao = padrao,
    arquivada = arquivada,
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)
