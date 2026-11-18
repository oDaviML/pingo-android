package com.pingo.core.network.update

data class ItemChangelog(val destaque: String?, val descricao: String)

data class SecaoChangelog(val titulo: String?, val itens: List<ItemChangelog>)

data class ChangelogFormatada(val secoes: List<SecaoChangelog>) {
    val vazia: Boolean
        get() = secoes.isEmpty() || secoes.all { it.itens.isEmpty() }
}

object FormatadorDeChangelog {
    private const val MAX_CARACTERES_ITEM = 140
    private const val MIN_POSICAO_PONTO = 28
    private const val MAX_ITENS_POR_SECAO = 8
    private const val MAX_SECOES = 3

    private val TITULOS_TECNICOS = setOf(
        "instalacao",
        "instalacao / atualizacao",
        "verificacao",
        "build",
        "assinatura",
        "checksum",
        "como instalar",
    )
    private val REGEX_CABECALHO = Regex("""^#{1,6}\s+(.+)$""")
    private val REGEX_MARCADOR = Regex("""^(?:[-*•]|\d+\.)\s+(.+)$""")
    private val REGEX_TICKETS = Regex("""\s*\(\s*STR-\d+(?:\s*,\s*STR-\d+)*\s*\)""", RegexOption.IGNORE_CASE)
    private val REGEX_LINK_MD = Regex("""\[([^\]]+)]\([^)]+\)""")
    private val REGEX_NEGRITO_COM_DOIS_PONTOS = Regex("""^\*\*(.+?):?\*\*\s*(?::|—|-)?\s*(.+)$""")

    fun formatar(notasBrutas: String): ChangelogFormatada {
        val estado = EstadoDoParser()
        val linhas = notasBrutas.replace("\r\n", "\n").lines()

        for (bruta in linhas) {
            if (!processarLinha(bruta, estado)) {
                break
            }
        }
        estado.fecharSecao()

        val secoes = estado.secoes
            .filter { it.itens.isNotEmpty() }
            .take(MAX_SECOES)
        if (secoes.isNotEmpty()) {
            return ChangelogFormatada(secoes)
        }
        return construirResumoSemLista(estado.paragrafosAvulsos)
    }

    private fun processarLinha(bruta: String, estado: EstadoDoParser): Boolean {
        val linha = bruta.trim()
        val tituloLimpo = REGEX_CABECALHO.matchEntire(linha)?.let { limparMarkdown(it.groupValues[1]) }
        val parar = deveEncerrarLeitura(linha, tituloLimpo, estado)
        if (!parar) {
            aplicarLinhaDeConteudo(linha, tituloLimpo, estado)
        }
        return !parar
    }

    private fun deveEncerrarLeitura(linha: String, tituloLimpo: String?, estado: EstadoDoParser): Boolean = when {
        linha == "---" -> estado.secoes.isNotEmpty() || estado.itensAtuais.isNotEmpty()
        tituloLimpo != null -> ehSecaoTecnica(tituloLimpo)
        else -> ehRodapeTecnico(linha)
    }

    private fun aplicarLinhaDeConteudo(linha: String, tituloLimpo: String?, estado: EstadoDoParser) {
        val marcador = REGEX_MARCADOR.matchEntire(linha)
        when {
            linha.isEmpty() -> estado.encerrarItemAtual()
            tituloLimpo != null -> estado.iniciarSecao(tituloLimpo)
            marcador != null -> estado.iniciarItem(marcador.groupValues[1])
            estado.itemPendente != null -> estado.anexarAoItem(linha)
            !ehIntroBoilerplate(linha) -> estado.paragrafosAvulsos.add(linha)
        }
    }

    private fun construirResumoSemLista(paragrafos: List<String>): ChangelogFormatada {
        val itens = paragrafos
            .map { limparMarkdown(it) }
            .filter { it.isNotBlank() && !ehRodapeTecnico(it) }
            .take(MAX_ITENS_POR_SECAO)
            .map { ItemChangelog(destaque = null, descricao = resumirTexto(it)) }
        return if (itens.isEmpty()) {
            ChangelogFormatada(emptyList())
        } else {
            ChangelogFormatada(listOf(SecaoChangelog(titulo = null, itens = itens)))
        }
    }

    private fun ehSecaoTecnica(titulo: String): Boolean {
        val chave = normalizarChave(titulo)
        return TITULOS_TECNICOS.any { chave == it || chave.startsWith("$it ") }
    }

    private fun ehRodapeTecnico(linha: String): Boolean {
        val normalizada = normalizarChave(limparMarkdown(linha))
        return normalizada.startsWith("build a partir do commit") ||
            normalizada.startsWith("sha-256") ||
            normalizada.startsWith("sha-1") ||
            normalizada.startsWith("applicationid")
    }

    private fun ehIntroBoilerplate(linha: String): Boolean {
        val normalizada = normalizarChave(limparMarkdown(linha))
        return normalizada.startsWith("apk release")
    }

    private fun interpretarItem(textoBruto: String): ItemChangelog? {
        val semTickets = REGEX_TICKETS.replace(textoBruto.replace("`", ""), "").trim()
        val matchDestaque = REGEX_NEGRITO_COM_DOIS_PONTOS.matchEntire(semTickets)
        if (matchDestaque != null) {
            val destaque = limparMarkdown(matchDestaque.groupValues[1]).trimEnd(':')
            val corpo = resumirTexto(limparMarkdown(matchDestaque.groupValues[2]))
            return if (destaque.isBlank() && corpo.isBlank()) {
                null
            } else {
                ItemChangelog(
                    destaque = destaque.takeIf { it.isNotBlank() },
                    descricao = corpo,
                )
            }
        }
        val limpo = resumirTexto(limparMarkdown(semTickets))
        return if (limpo.isBlank()) null else ItemChangelog(destaque = null, descricao = limpo)
    }

    private fun limparMarkdown(texto: String): String = texto
        .replace("`", "")
        .let { REGEX_TICKETS.replace(it, "") }
        .let { REGEX_LINK_MD.replace(it, "$1") }
        .replace("**", "")
        .replace("__", "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()

    private fun resumirTexto(texto: String): String {
        if (texto.length <= MAX_CARACTERES_ITEM) {
            return texto
        }
        val corteFrase = encontrarFimDeFrase(texto)
        if (corteFrase != null) {
            return texto.substring(0, corteFrase).trimEnd('.', ';', ',', ' ') + "."
        }
        val limite = texto.lastIndexOf(' ', startIndex = MAX_CARACTERES_ITEM)
            .takeIf { it >= MIN_POSICAO_PONTO }
            ?: MAX_CARACTERES_ITEM
        return texto.substring(0, limite).trimEnd('.', ';', ',', ' ') + "…"
    }

    private fun encontrarFimDeFrase(texto: String): Int? {
        val candidato = listOf(". ", "; ")
            .mapNotNull { separador ->
                val indice = texto.indexOf(separador)
                indice.takeIf { it in MIN_POSICAO_PONTO until MAX_CARACTERES_ITEM }
            }
            .minOrNull()
        return candidato
    }

    private fun normalizarChave(texto: String): String = texto
        .lowercase()
        .replace("ã", "a")
        .replace("á", "a")
        .replace("à", "a")
        .replace("â", "a")
        .replace("ç", "c")
        .replace("é", "e")
        .replace("ê", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ô", "o")
        .replace("õ", "o")
        .replace("ú", "u")
        .trim()

    private class EstadoDoParser {
        val secoes = mutableListOf<SecaoChangelog>()
        val itensAtuais = mutableListOf<ItemChangelog>()
        val paragrafosAvulsos = mutableListOf<String>()
        var tituloAtual: String? = null
        var itemPendente: StringBuilder? = null

        fun iniciarSecao(titulo: String) {
            fecharSecao()
            tituloAtual = titulo
        }

        fun iniciarItem(texto: String) {
            encerrarItemAtual()
            itemPendente = StringBuilder(texto.trim())
        }

        fun anexarAoItem(linha: String) {
            val sb = itemPendente ?: return
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(linha.trim())
        }

        fun encerrarItemAtual() {
            val texto = itemPendente?.toString().orEmpty().trim()
            itemPendente = null
            if (texto.isNotEmpty() && itensAtuais.size < MAX_ITENS_POR_SECAO) {
                interpretarItem(texto)?.let(itensAtuais::add)
            }
        }

        fun fecharSecao() {
            encerrarItemAtual()
            if (itensAtuais.isNotEmpty()) {
                secoes.add(SecaoChangelog(titulo = tituloAtual, itens = itensAtuais.toList()))
                itensAtuais.clear()
            }
        }
    }
}
