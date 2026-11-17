package com.pingo.core.network.sync

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampoPresencaTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `campo ausente e distinto de nulo explicito`() {
        val ausente = json.decodeFromString(InstrumentoSnapshotDto.serializer(), instrumentoJson())
        val nulo = json.decodeFromString(
            InstrumentoSnapshotDto.serializer(),
            instrumentoJson("\"fechamentoDia\": null,"),
        )

        assertFalse(ausente.fechamentoDia.presente)
        assertNull(ausente.fechamentoDia.valor)

        assertTrue(nulo.fechamentoDia.presente)
        assertNull(nulo.fechamentoDia.valor)
    }

    @Test
    fun `valores presentes chegam tipados nos campos financeiros`() {
        val snapshot = json.decodeFromString(
            InstrumentoSnapshotDto.serializer(),
            instrumentoJson(
                """
                "ultimosDigitos": "1234",
                "fechamentoDia": 5,
                "vencimentoDia": 12,
                "beneficio": "VR",
                "saldo": 486.20,
                "diaRecarga": 5,
                "valorRecarga": 300.0,
                "limiteCredito": 5000.0,
                "arquivadoPorCascata": true,
                """.trimIndent(),
            ),
        )

        assertEquals("1234", snapshot.ultimosDigitos.valor)
        assertEquals(5, snapshot.fechamentoDia.valor)
        assertEquals(12, snapshot.vencimentoDia.valor)
        assertEquals("VR", snapshot.beneficio.valor)
        assertEquals(486.20, snapshot.saldo.valor ?: 0.0, 0.001)
        assertEquals(5, snapshot.diaRecarga.valor)
        assertEquals(300.0, snapshot.valorRecarga.valor ?: 0.0, 0.001)
        assertEquals(5000.0, snapshot.limiteCredito.valor ?: 0.0, 0.001)
        assertEquals(true, snapshot.arquivadoPorCascata.valor)
    }

    private fun instrumentoJson(camposOpcionais: String = ""): String = """
        {
          "publicId": "instrumento-1",
          "contaPublicId": "conta-1",
          "tipo": "Credito",
          "apelido": "Cartão",
          "arquivado": false,
          ${camposOpcionais.trim()}
          "criadoEm": "2026-11-10T12:00:00Z",
          "atualizadoEm": "2026-11-10T12:00:00Z"
        }
    """.trimIndent()
}
