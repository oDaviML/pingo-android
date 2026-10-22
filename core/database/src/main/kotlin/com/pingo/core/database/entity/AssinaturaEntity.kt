package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.StatusAssinatura
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "assinaturas",
    indices = [Index("atualizadoEm"), Index("excluidoEm")],
)
data class AssinaturaEntity(
    @PrimaryKey val publicId: String,
    val estabelecimento: String,
    val estabelecimentoNormalizado: String,
    val valorReferenciaCentavos: Long,
    val ultimaCobranca: LocalDate?,
    val proximaCobrancaEstimada: LocalDate?,
    val status: StatusAssinatura,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
