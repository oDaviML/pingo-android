package com.pingo.sync

import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.network.sync.AssinaturaChangeDto
import com.pingo.core.network.sync.AssinaturaSnapshotDto
import java.time.LocalDate

internal fun AssinaturaEntity.toChangeDto(): AssinaturaChangeDto = AssinaturaChangeDto(
    publicId = publicId,
    estabelecimento = estabelecimento,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    valorReferencia = valorReferenciaCentavos.toApiReais(),
    ultimaCobranca = ultimaCobranca?.toApiValue(),
    proximaCobrancaEstimada = proximaCobrancaEstimada?.toApiValue(),
    status = status.toApiValue(),
    excluidoEm = excluidoEm?.toApiValue(),
    instrumentoPublicId = instrumentoPublicId,
)

internal fun AssinaturaSnapshotDto.toEntity(local: AssinaturaEntity? = null): AssinaturaEntity = AssinaturaEntity(
    publicId = publicId,
    estabelecimento = estabelecimento,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    valorReferenciaCentavos = valorReferencia.toCentavos(),
    ultimaCobranca = ultimaCobranca?.let(LocalDate::parse),
    proximaCobrancaEstimada = proximaCobrancaEstimada?.let(LocalDate::parse),
    status = statusAssinaturaFromApi(status),
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
    instrumentoPublicId = instrumentoPublicId.resolver { local?.instrumentoPublicId },
)
