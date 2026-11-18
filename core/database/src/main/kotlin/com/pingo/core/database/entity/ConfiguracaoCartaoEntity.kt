package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "configuracoes_cartao",
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
        Index("instrumentoPublicId", "vigenciaInicio"),
        Index("atualizadoEm"),
        Index("excluidoEm"),
    ],
)
data class ConfiguracaoCartaoEntity(
    @PrimaryKey val publicId: String,
    val instrumentoPublicId: String,
    val vigenciaInicio: Instant,
    val fechamentoDia: Int?,
    val vencimentoDia: Int?,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
