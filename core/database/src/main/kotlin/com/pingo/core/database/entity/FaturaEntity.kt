package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.StatusFatura
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "faturas",
    foreignKeys = [
        ForeignKey(
            entity = InstrumentoEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["instrumentoPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("instrumentoPublicId"),
        Index("pagamentoLancamentoPublicId"),
        Index("competencia"),
        Index("atualizadoEm"),
        Index("excluidoEm"),
    ],
)
data class FaturaEntity(
    @PrimaryKey val publicId: String,
    val instrumentoPublicId: String,
    val competencia: LocalDate,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
    val totalCentavos: Long,
    val totalPagoCentavos: Long,
    val status: StatusFatura,
    val dataPagamento: LocalDate?,
    val pagamentoLancamentoPublicId: String?,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
