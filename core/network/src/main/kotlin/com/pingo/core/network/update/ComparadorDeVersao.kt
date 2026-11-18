package com.pingo.core.network.update

data class VersaoSemantica(val major: Int, val minor: Int, val patch: Int, val preRelease: List<String> = emptyList()) :
    Comparable<VersaoSemantica> {
    override fun compareTo(other: VersaoSemantica): Int {
        val diferencaNucleo = compareValuesBy(
            this,
            other,
            VersaoSemantica::major,
            VersaoSemantica::minor,
            VersaoSemantica::patch,
        )
        if (diferencaNucleo != 0) return diferencaNucleo
        return compararPreReleases(preRelease, other.preRelease)
    }

    private fun compararPreReleases(primeira: List<String>, segunda: List<String>): Int = when {
        primeira.isEmpty() && segunda.isEmpty() -> 0
        primeira.isEmpty() -> 1
        segunda.isEmpty() -> -1
        else -> {
            val tamanhoComum = minOf(primeira.size, segunda.size)
            for (indice in 0 until tamanhoComum) {
                val diferenca = compararIdentificador(primeira[indice], segunda[indice])
                if (diferenca != 0) return diferenca
            }
            primeira.size.compareTo(segunda.size)
        }
    }

    private fun compararIdentificador(primeiro: String, segundo: String): Int {
        val numeroPrimeiro = primeiro.takeIf { it.all(Char::isDigit) }?.toLongOrNull()
        val numeroSegundo = segundo.takeIf { it.all(Char::isDigit) }?.toLongOrNull()
        return when {
            numeroPrimeiro != null && numeroSegundo != null -> numeroPrimeiro.compareTo(numeroSegundo)
            numeroPrimeiro != null -> -1
            numeroSegundo != null -> 1
            else -> primeiro.compareTo(segundo)
        }
    }
}

object ComparadorDeVersao {
    private val REGEX_DIGITOS = Regex("^\\d+$")
    private val REGEX_PRE_RELEASE = Regex("^[0-9A-Za-z-]+$")

    fun normalizar(versao: String): String = versao.trim().removePrefix("v").removePrefix("V")

    fun parse(versao: String): VersaoSemantica? {
        val semBuild = normalizar(versao).substringBefore('+')
        if (semBuild.isBlank()) return null

        val indiceTraco = semBuild.indexOf('-')
        val nucleoTexto = if (indiceTraco >= 0) semBuild.substring(0, indiceTraco) else semBuild
        val preTexto = if (indiceTraco >= 0) semBuild.substring(indiceTraco + 1) else ""
        val partesNucleo = parsePartesNucleo(nucleoTexto)
        val preRelease = if (indiceTraco >= 0 && preTexto.isBlank()) {
            null
        } else if (preTexto.isEmpty()) {
            emptyList()
        } else {
            parsePreRelease(preTexto)
        }

        return if (partesNucleo != null && preRelease != null) {
            VersaoSemantica(
                major = partesNucleo[0],
                minor = partesNucleo[1],
                patch = partesNucleo[2],
                preRelease = preRelease,
            )
        } else {
            null
        }
    }

    fun comparar(primeira: String, segunda: String): Int? {
        val a = parse(primeira) ?: return null
        val b = parse(segunda) ?: return null
        return a.compareTo(b)
    }

    fun ehMaisRecente(candidata: String, instalada: String): Boolean = (comparar(candidata, instalada) ?: 0) > 0

    private fun parsePartesNucleo(nucleoTexto: String): List<Int>? {
        val segmentos = nucleoTexto.split('.')
        val numeros = if (segmentos.size in 2..3) {
            segmentos.mapNotNull { segmento ->
                segmento.takeIf { REGEX_DIGITOS.matches(it) }?.toIntOrNull()
            }
        } else {
            emptyList()
        }
        return when {
            numeros.size != segmentos.size || segmentos.isEmpty() -> null
            numeros.size == 2 -> numeros + 0
            else -> numeros
        }
    }

    private fun parsePreRelease(preTexto: String): List<String>? {
        val identificadores = preTexto.split('.')
        val valido = identificadores.all { it.isNotEmpty() && REGEX_PRE_RELEASE.matches(it) }
        return if (valido) identificadores else null
    }
}
