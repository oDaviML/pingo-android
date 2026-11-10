package com.pingo.sync

import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.network.sync.LancamentoChangeDto
import com.pingo.core.network.sync.LancamentoSnapshotDto

internal fun LancamentoEntity.toChangeDto(): LancamentoChangeDto = LancamentoChangeDto(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    faturaPublicId = faturaPublicId,
    assinaturaPublicId = assinaturaPublicId,
    compraPaiPublicId = compraPaiPublicId,
    estornoDePublicId = estornoDePublicId,
    transferenciaId = transferenciaId,
    valor = valorCentavos.toApiReais(),
    dataHora = dataHora.toApiValue(),
    tipo = tipo.toApiValue(),
    estabelecimento = estabelecimento,
    status = status.toApiValue(),
    origem = origem.toApiValue(),
    confianca = confianca,
    motivoPendencia = motivoPendencia,
    hashDuplicata = hashDuplicata,
    observacao = observacao,
    parcelaNumero = parcelaNumero,
    totalParcelas = totalParcelas,
    valorTotalCompra = valorTotalCompraCentavos?.toApiReais(),
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun LancamentoSnapshotDto.toEntity(): LancamentoEntity = LancamentoEntity(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    faturaPublicId = faturaPublicId,
    assinaturaPublicId = assinaturaPublicId,
    compraPaiPublicId = compraPaiPublicId,
    estornoDePublicId = estornoDePublicId,
    transferenciaId = transferenciaId,
    valorCentavos = valor.toCentavos(),
    dataHora = dataHora.toInstantValue(),
    tipo = tipoLancamentoFromApi(tipo),
    estabelecimento = estabelecimento,
    status = statusLancamentoFromApi(status),
    origem = origemLancamentoFromApi(origem),
    confianca = confianca,
    motivoPendencia = motivoPendencia,
    hashDuplicata = hashDuplicata,
    observacao = observacao,
    parcelaNumero = parcelaNumero,
    totalParcelas = totalParcelas,
    valorTotalCompraCentavos = valorTotalCompra?.toCentavos(),
    criadoEm = criadoEm.toInstantValue(),
    atualizadoEm = atualizadoEm.toInstantValue(),
    excluidoEm = excluidoEm?.toInstantValue(),
    textoBruto = null,
)
