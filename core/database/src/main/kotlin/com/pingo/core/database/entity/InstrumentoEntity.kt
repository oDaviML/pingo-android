package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.TipoInstrumento
import java.time.Instant

@Entity(
    tableName = "instrumentos",
    foreignKeys = [
        ForeignKey(
            entity = ContaEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["contaPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("contaPublicId"), Index("atualizadoEm"), Index("excluidoEm")],
)
data class InstrumentoEntity(
    @PrimaryKey val publicId: String,
    val contaPublicId: String,
    val tipo: TipoInstrumento,
    val apelido: String,
    val ultimosDigitos: String?,
    val arquivado: Boolean,
    val fechamentoDia: Int?,
    val vencimentoDia: Int?,
    val beneficio: BeneficioVoucher?,
    val saldoCentavos: Long?,
    val diaRecarga: Int?,
    val valorRecargaCentavos: Long?,
    val limiteCreditoCentavos: Long?,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
