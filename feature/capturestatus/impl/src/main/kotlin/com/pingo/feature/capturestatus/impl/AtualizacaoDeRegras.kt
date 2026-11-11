package com.pingo.feature.capturestatus.impl

import com.pingo.core.notifications.AtualizadorDeRegras
import com.pingo.core.notifications.ResultadoAtualizacaoRegras
import javax.inject.Inject
import javax.inject.Singleton

interface AtualizacaoDeRegras {
    suspend fun atualizar(): ResultadoAtualizacaoRegras
}

@Singleton
class AtualizacaoDeRegrasDoPingo @Inject constructor(private val atualizador: AtualizadorDeRegras) : AtualizacaoDeRegras {
    override suspend fun atualizar(): ResultadoAtualizacaoRegras = atualizador.atualizar()
}
