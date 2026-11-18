package com.pingo.core.data

interface TransacaoLocal {
    suspend fun <T> executar(bloco: suspend () -> T): T

    object Direta : TransacaoLocal {
        override suspend fun <T> executar(bloco: suspend () -> T): T = bloco()
    }
}
