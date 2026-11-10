package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object HashDuplicata {
    private const val SEPARADOR = "|"
    private val formatoDoMinuto = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC)

    fun de(instituicao: InstituicaoFinanceira, valorCentavos: Long, estabelecimentoNormalizado: String?, em: Instant): String {
        val material = listOf(
            instituicao.name,
            valorCentavos.toString(),
            estabelecimentoNormalizado.orEmpty(),
            formatoDoMinuto.format(em),
        ).joinToString(SEPARADOR)

        return MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8)).emHexadecimal()
    }

    private fun ByteArray.emHexadecimal(): String = joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
}
