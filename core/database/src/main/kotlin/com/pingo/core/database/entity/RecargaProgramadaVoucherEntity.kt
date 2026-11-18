package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.voucher.StatusRecargaProgramada
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "recargas_programadas_voucher",
    foreignKeys = [
        ForeignKey(
            entity = InstrumentoEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["instrumentoPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = LancamentoEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["lancamentoPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("instrumentoPublicId"),
        Index("lancamentoPublicId"),
        Index("instrumentoPublicId", "competenciaMes"),
        Index("atualizadoEm"),
        Index("excluidoEm"),
    ],
)
data class RecargaProgramadaVoucherEntity(
    @PrimaryKey val publicId: String,
    val instrumentoPublicId: String,
    val beneficio: BeneficioVoucher?,
    val competenciaMes: LocalDate,
    val dataPrevista: LocalDate,
    val valorPrevistoCentavos: Long,
    val status: StatusRecargaProgramada,
    val lancamentoPublicId: String?,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
