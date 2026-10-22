package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "usuarios",
    indices = [Index("sub")],
)
data class UsuarioEntity(
    @PrimaryKey val publicId: String,
    val sub: String?,
    val email: String,
    val nome: String?,
    val consentimentoLgpdEm: Instant?,
    val consentimentoLgpdVersao: String?,
    val telemetriaOptIn: Boolean,
    val telemetriaOptInEm: Instant?,
    val aparelhoAtivoId: String?,
    val aparelhoAtivoNome: String?,
    val aparelhoAtivoEm: Instant?,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
)
