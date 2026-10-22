package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "regras_categoria",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["categoriaPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("categoriaPublicId"), Index("atualizadoEm"), Index("excluidoEm")],
)
data class RegraCategoriaEntity(
    @PrimaryKey val publicId: String,
    val categoriaPublicId: String,
    val estabelecimentoNormalizado: String,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
