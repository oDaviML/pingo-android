package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.InstituicaoFinanceira
import java.time.Instant

@Entity(
    tableName = "regras_parser",
    indices = [
        Index(value = ["instituicao", "versao"], unique = true),
        Index("atualizadoEm"),
    ],
)
data class RegraParserEntity(
    @PrimaryKey val publicId: String,
    val instituicao: InstituicaoFinanceira,
    val versao: Int,
    val padroes: String,
    val ativa: Boolean,
    val publicadaEm: Instant,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
)
