package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.TipoCategoria
import java.time.Instant

@Entity(
    tableName = "categorias",
    indices = [Index("atualizadoEm"), Index("excluidoEm")],
)
data class CategoriaEntity(
    @PrimaryKey val publicId: String,
    val nome: String,
    val tipo: TipoCategoria,
    val padrao: Boolean,
    val arquivada: Boolean,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
