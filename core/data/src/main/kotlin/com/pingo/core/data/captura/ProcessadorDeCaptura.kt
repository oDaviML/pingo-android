package com.pingo.core.data.captura

interface ProcessadorDeCaptura {
    suspend fun processar(captura: CapturaParaProcessar): ResultadoProcessamento
}
