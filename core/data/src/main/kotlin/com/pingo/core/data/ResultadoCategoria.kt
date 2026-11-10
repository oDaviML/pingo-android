package com.pingo.core.data

import com.pingo.core.model.Categoria

sealed interface ResultadoCategoria {
    data class Sucesso(val categoria: Categoria) : ResultadoCategoria

    data object NomeVazio : ResultadoCategoria

    data object NomeEmUso : ResultadoCategoria

    data object NaoEncontrada : ResultadoCategoria
}
