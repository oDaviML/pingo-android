package com.pingo.core.data

interface TransacaoLocal {
    suspend fun <T> executar(bloco: suspend () -> T): T
}
