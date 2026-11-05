package com.pingo.sync

import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.network.sync.FaturaChangeDto
import com.pingo.core.network.sync.FaturaSnapshotDto
import java.time.LocalDate

internal fun FaturaEntity.toChangeDto(): FaturaChangeDto = FaturaChangeDto(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    competencia = competencia.toApiValue(),
    dataFechamento = dataFechamento.toApiValue(),
    dataVencimento = dataVencimento.toApiValue(),
    total = totalCentavos.toApiReais(),
    totalPago = totalPagoCentavos.toApiReais(),
    status = status.toApiValue(),
    dataPagamento = dataPagamento?.toApiValue(),
    pagamentoLancamentoPublicId = pagamentoLancamentoPublicId,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun FaturaSnapshotDto.toEntity(): FaturaEntity = FaturaEntity(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    competencia = LocalDate.parse(competencia),
    dataFechamento = LocalDate.parse(dataFechamento),
    dataVencimento = LocalDate.parse(dataVencimento),
    totalCentavos = total.toCentavos(),
    totalPagoCentavos = totalPago.toCentavos(),
    status = statusFaturaFromApi(status),
    dataPagamento = dataPagamento?.let(LocalDate::parse),
    pagamentoLancamentoPublicId = pagamentoLancamentoPublicId,
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
)
