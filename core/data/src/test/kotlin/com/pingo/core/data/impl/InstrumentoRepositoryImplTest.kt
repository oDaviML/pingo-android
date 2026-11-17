package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.TipoInstrumento
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentoRepositoryImplTest {
    private val instrumentoDao = FakeInstrumentoDao()
    private val repositorio = InstrumentoRepositoryImpl(instrumentoDao)

    @Test
    fun `arquivar instrumento isolado preserva a conta e os irmaos`() = runBlocking {
        seed(publicId = "instrumento-1", apelido = "Principal")
        seed(publicId = "instrumento-2", apelido = "Adicional")

        repositorio.arquivar("instrumento-1", INSTANTE_DA_EDICAO)

        val arquivado = repositorio.buscar("instrumento-1")
        val irmao = repositorio.buscar("instrumento-2")
        assertTrue(arquivado?.arquivado == true)
        assertFalse(arquivado?.arquivadoPorCascata == true)
        assertEquals(INSTANTE_DA_EDICAO, arquivado?.atualizadoEm)
        assertFalse(irmao?.arquivado == true)
        assertEquals(INSTANTE_DA_CRIACAO, irmao?.atualizadoEm)
    }

    @Test
    fun `reativar instrumento isolado devolve elegibilidade e limpa o marcador`() = runBlocking {
        seed(publicId = "instrumento-1", apelido = "Principal")
        instrumentoDao.arquivarAtivosPorCascata(CONTA, INSTANTE_DA_EDICAO)

        repositorio.reativar("instrumento-1", INSTANTE_DA_EDICAO.plusSeconds(60))

        val reativado = repositorio.buscar("instrumento-1")
        assertFalse(reativado?.arquivado == true)
        assertFalse(reativado?.arquivadoPorCascata == true)
        assertEquals(INSTANTE_DA_EDICAO.plusSeconds(60), reativado?.atualizadoEm)
    }

    private suspend fun seed(publicId: String, apelido: String) {
        instrumentoDao.upsert(
            InstrumentoEntity(
                publicId = publicId,
                contaPublicId = CONTA,
                tipo = TipoInstrumento.CREDITO,
                apelido = apelido,
                ultimosDigitos = "1234",
                arquivado = false,
                fechamentoDia = 5,
                vencimentoDia = 12,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = INSTANTE_DA_CRIACAO,
                atualizadoEm = INSTANTE_DA_CRIACAO,
                excluidoEm = null,
            ),
        )
    }

    private companion object {
        const val CONTA = "conta-1"
    }
}
