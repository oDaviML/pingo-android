package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

internal data class EntradaCorpus(
    val arquivo: String,
    val ordem: Int,
    val timestamp: String,
    val app: String,
    val pacote: String,
    val titulo: String,
    val texto: String,
    val bigText: String,
) {
    val vazia: Boolean
        get() = titulo.isBlank() && texto.isBlank() && bigText.isBlank()

    fun notificacao(): NotificacaoBruta = NotificacaoBruta(titulo = titulo, texto = texto, bigText = bigText)
}

internal enum class ClasseEsperada { TRANSACIONAL, RUIDO, VAZIO, FORA_V1, NEGADA, NAO_BANCARIO }

internal data class ExpectativaCorpus(
    val arquivo: String,
    val ordem: Int,
    val timestamp: String,
    val pacote: String,
    val instituicao: String,
    val esperado: ClasseEsperada,
    val tipo: String,
    val motivo: String,
)

internal object CorpusFixtures {
    private val arquivos = listOf("pingo-corpus.txt", "pingo-corpus-iasmin.txt", "pingo-corpus-kaua.txt")

    fun entradas(): List<EntradaCorpus> = arquivos.flatMap { arquivo -> parseCorpus(arquivo, recurso("corpus/$arquivo")) }

    fun expectativas(): List<ExpectativaCorpus> =
        recurso("corpus/expectativas.tsv").lines().drop(1).filter(String::isNotBlank).map(::parseExpectativa)

    fun regrasFixture(): Map<InstituicaoFinanceira, VersaoRegras> = RegrasVigentes.selecionar(regrasDoFixture())

    private fun regrasDoFixture(): List<RegraParser> =
        Json.parseToJsonElement(recurso("regras/regras-fixture.json")).jsonObject.map { (instituicao, padroes) ->
            regraParser(InstituicaoFinanceira.valueOf(instituicao), 1, padroes.toString())
        }

    private fun recurso(caminho: String): String =
        requireNotNull(CorpusFixtures::class.java.classLoader.getResource(caminho)) { "recurso ausente: $caminho" }.readText()
}

private val cabecalhoEntrada = Regex("""(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}) \u00B7 (.+) \(([^()]+)\)""")
private val rotuloCampo = Regex("""(Título|Texto|BigText): ?(.*)""", RegexOption.DOT_MATCHES_ALL)

internal fun parseCorpus(arquivo: String, conteudo: String): List<EntradaCorpus> = agruparPorCabecalho(conteudo.lines())
    .mapIndexed { indice, linhas -> parseEntrada(arquivo, indice + 1, linhas) }

private fun agruparPorCabecalho(linhas: List<String>): List<List<String>> {
    val entradas = mutableListOf<MutableList<String>>()
    linhas.filter(String::isNotBlank).forEach { linha ->
        if (cabecalhoEntrada.matches(linha)) {
            entradas.add(mutableListOf(linha))
        } else {
            val atual = requireNotNull(entradas.lastOrNull()) { "linha antes do primeiro cabeçalho: $linha" }
            atual.add(linha)
        }
    }
    return entradas
}

private fun parseEntrada(arquivo: String, ordem: Int, linhas: List<String>): EntradaCorpus {
    val cabecalho = requireNotNull(cabecalhoEntrada.matchEntire(linhas.first())) { "cabeçalho inválido em $arquivo#$ordem" }
    val campos = camposDe(linhas.drop(1))
    fun campo(rotulo: String): String = requireNotNull(campos[rotulo]) { "campo $rotulo ausente em $arquivo#$ordem" }
    return EntradaCorpus(
        arquivo = arquivo,
        ordem = ordem,
        timestamp = cabecalho.groupValues[1],
        app = cabecalho.groupValues[2],
        pacote = cabecalho.groupValues[3],
        titulo = campo("Título"),
        texto = campo("Texto"),
        bigText = campo("BigText"),
    )
}

private fun camposDe(linhas: List<String>): Map<String, String> {
    val campos = linkedMapOf<String, StringBuilder>()
    var rotuloAtual: String? = null
    for (linha in linhas) {
        val rotulo = rotuloCampo.matchEntire(linha)
        if (rotulo != null) {
            rotuloAtual = rotulo.groupValues[1]
            campos[rotulo.groupValues[1]] = StringBuilder(rotulo.groupValues[2])
        } else {
            val rotuloCorrente = requireNotNull(rotuloAtual) { "linha sem rótulo: $linha" }
            campos.getValue(rotuloCorrente).append('\n').append(linha)
        }
    }
    return campos.mapValues { (_, valor) -> valor.toString() }
}

private fun parseExpectativa(linha: String): ExpectativaCorpus {
    val colunas = linha.split('\t')
    require(colunas.size == 8) { "linha de expectativa inválida: $linha" }
    return ExpectativaCorpus(
        arquivo = colunas[0],
        ordem = colunas[1].toInt(),
        timestamp = colunas[2],
        pacote = colunas[3],
        instituicao = colunas[4],
        esperado = ClasseEsperada.valueOf(colunas[5]),
        tipo = colunas[6],
        motivo = colunas[7],
    )
}
