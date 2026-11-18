package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import java.time.Instant

@Entity(
    tableName = "referencias_saldo_voucher",
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
        Index("instrumentoPublicId", "instanteReferencia"),
        Index("atualizadoEm"),
        Index("excluidoEm"),
    ],
)
data class ReferenciaSaldoVoucherEntity(
    @PrimaryKey val publicId: String,
    val instrumentoPublicId: String,
    val tipoReferencia: TipoReferenciaSaldoVoucher,
    val saldoCentavos: Long,
    val deltaCentavos: Long?,
    val instanteReferencia: Instant,
    val lancamentoPublicId: String?,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
