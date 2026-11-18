package com.pingo.sync

import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.network.sync.RecargaProgramadaVoucherChangeDto
import com.pingo.core.network.sync.RecargaProgramadaVoucherSnapshotDto
import java.time.LocalDate

internal fun RecargaProgramadaVoucherEntity.toChangeDto(): RecargaProgramadaVoucherChangeDto = RecargaProgramadaVoucherChangeDto(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    beneficio = beneficio?.toApiValue(),
    competenciaMes = competenciaMes.toApiValue(),
    dataPrevista = dataPrevista.toApiValue(),
    valorPrevistoCentavos = valorPrevistoCentavos,
    status = status.toApiValue(),
    lancamentoPublicId = lancamentoPublicId,
    excluidoEm = excluidoEm?.toApiValue(),
)

internal fun RecargaProgramadaVoucherSnapshotDto.toEntity(local: RecargaProgramadaVoucherEntity? = null): RecargaProgramadaVoucherEntity =
    RecargaProgramadaVoucherEntity(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        beneficio = beneficio.resolver { local?.beneficio?.toApiValue() }?.let(::beneficioFromApi),
        competenciaMes = LocalDate.parse(competenciaMes),
        dataPrevista = LocalDate.parse(dataPrevista),
        valorPrevistoCentavos = valorPrevistoCentavos,
        status = statusRecargaFromApi(status),
        lancamentoPublicId = lancamentoPublicId,
        criadoEm = criadoEm.toInstantValue(),
        atualizadoEm = atualizadoEm.toInstantValue(),
        excluidoEm = excluidoEm?.toInstantValue(),
    )
