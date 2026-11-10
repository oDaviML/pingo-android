package com.pingo.core.data.categoria

import com.pingo.core.model.Categoria
import com.pingo.core.model.TipoCategoria

private val espacosRepetidos = Regex("\\s+")

internal fun String.nomeLimpo(): String = trim().replace(espacosRepetidos, " ")

internal fun Categoria.ocupaNome(nome: String, tipo: TipoCategoria): Boolean =
    this.tipo == tipo && this.nome.equals(nome, ignoreCase = true)
