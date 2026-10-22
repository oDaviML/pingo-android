package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import java.time.Instant

@Entity(
    tableName = "contas",
    indices = [Index("atualizadoEm"), Index("excluidoEm")],
)
data class ContaEntity(
    @PrimaryKey val publicId: String,
    val instituicao: InstituicaoFinanceira,
    val apelido: String,
    val tipo: TipoConta,
    val arquivada: Boolean,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
