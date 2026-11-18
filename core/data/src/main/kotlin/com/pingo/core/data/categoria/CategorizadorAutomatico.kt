package com.pingo.core.data.categoria

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.model.Categoria
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.first
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

class CategorizadorAutomatico @Inject constructor(private val categoriaRepository: CategoriaRepository) {
    suspend fun sugerir(estabelecimentoBruto: String?, tipo: TipoCategoria, beneficioBruto: String? = null): Categoria? {
        val categorias = categoriaRepository.observeEscolhiveis().first()
        return sugerirNaLista(
            estabelecimentoBruto = estabelecimentoBruto,
            tipo = tipo,
            beneficioBruto = beneficioBruto,
            categorias = categorias,
        )
    }

    fun sugerirNaLista(
        estabelecimentoBruto: String?,
        tipo: TipoCategoria,
        beneficioBruto: String? = null,
        categorias: List<Categoria>,
    ): Categoria? {
        val nomeSugerido = nomeSugerido(estabelecimentoBruto, tipo, beneficioBruto) ?: return null
        val alvo = normalizar(nomeSugerido)
        return categorias.firstOrNull { categoria ->
            categoria.podeSerEscolhida &&
                categoria.tipo == tipo &&
                normalizar(categoria.nome) == alvo
        }
    }

    internal fun nomeSugerido(estabelecimentoBruto: String?, tipo: TipoCategoria, beneficioBruto: String? = null): String? {
        val texto = limparEstabelecimento(estabelecimentoBruto)
        val nomePorTexto = if (texto != null) {
            val regras = if (tipo == TipoCategoria.DESPESA) PadroesDeCategoria.regrasDespesa else PadroesDeCategoria.regrasReceita
            buscarRegraCorrespondente(texto, regras)
        } else {
            null
        }
        return nomePorTexto ?: sugerirPorBeneficio(beneficioBruto, tipo)
    }

    private fun buscarRegraCorrespondente(texto: String, regras: List<RegraPadraoCategoria>): String? {
        val tokens = texto.split(' ').filter { it.isNotEmpty() }
        val preenchido = " $texto "
        for (regra in regras) {
            if (regra.termosExatos.any { termo -> preenchido.contains(" $termo ") }) {
                return regra.categoriaNome
            }
            if (regra.prefixosLongos.any { prefixo -> tokens.any { token -> token.startsWith(prefixo) } }) {
                return regra.categoriaNome
            }
        }
        return null
    }

    private fun sugerirPorBeneficio(beneficioBruto: String?, tipo: TipoCategoria): String? {
        if (tipo != TipoCategoria.DESPESA || beneficioBruto.isNullOrBlank()) {
            return null
        }
        val beneficio = normalizar(beneficioBruto)
        return when {
            "mobilidade" in beneficio || "transporte" in beneficio || beneficio == "vt" -> "Transporte"
            "refeicao" in beneficio || "alimentacao" in beneficio || beneficio == "vr" || beneficio == "va" -> "Alimentação"
            "cultura" in beneficio -> "Lazer"
            "saude" in beneficio -> "Saúde"
            "educacao" in beneficio -> "Educação"
            else -> null
        }
    }

    private fun limparEstabelecimento(bruto: String?): String? {
        val limpo = bruto?.let(::normalizar) ?: return null
        val tokens = limpo.split(' ').filter { it.isNotEmpty() }
        val semPrefixo = if (tokens.size >= 2 && tokens.first() in PREFIXOS_ADQUIRENTE) {
            tokens.drop(1).joinToString(" ")
        } else {
            tokens.joinToString(" ")
        }
        return semPrefixo.takeIf { it.isNotEmpty() }
    }

    private fun normalizar(texto: String): String = Normalizer
        .normalize(texto, Normalizer.Form.NFD)
        .replace(MARCAS_ACENTUACAO, "")
        .lowercase(LOCALE_PT_BR)
        .replace(SEPARADORES, " ")
        .replace(ESPACOS, " ")
        .trim()

    private companion object {
        val LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")
        val MARCAS_ACENTUACAO = Regex("\\p{M}+")
        val SEPARADORES = Regex("[*\\-_/.,:()]+")
        val ESPACOS = Regex("\\s+")
        val PREFIXOS_ADQUIRENTE = setOf(
            "pag", "pagseguro", "pg", "mp", "mercpago", "picpay",
            "ebn", "ebanx", "stone", "sumup", "cielo", "rede", "getnet",
            "dl", "sq",
        )
    }
}
