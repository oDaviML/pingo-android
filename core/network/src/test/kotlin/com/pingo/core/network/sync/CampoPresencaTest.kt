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

    @Test
    fun `assinatura snapshot distingue ausencia de instrumentoPublicId`() {
        val subAusente = json.decodeFromString(
            AssinaturaSnapshotDto.serializer(),
            """
            {
              "publicId": "sub-1",
              "estabelecimento": "Spotify",
              "estabelecimentoNormalizado": "SPOTIFY",
              "valorReferencia": 21.9,
              "status": "Confirmada",
              "criadoEm": "2026-11-10T12:00:00Z",
              "atualizadoEm": "2026-11-10T12:00:00Z"
            }
            """.trimIndent(),
        )
        assertFalse(subAusente.instrumentoPublicId.presente)
    }

    @Test
    fun `lancamento e recarga snapshots preservam presenca de campos F2`() {
        val lancPresente = json.decodeFromString(
            LancamentoSnapshotDto.serializer(),
            """
            {
              "publicId": "lanc-1",
              "valor": 45.0,
              "dataHora": "2026-11-10T12:00:00Z",
              "tipo": "Despesa",
              "status": "Confirmado",
              "origem": "Notificacao",
              "confianca": 1.0,
              "criadoEm": "2026-11-10T12:00:00Z",
              "atualizadoEm": "2026-11-10T12:00:00Z",
              "estabelecimentoNormalizado": "PADARIA",
              "beneficio": null,
              "saldoObservadoCentavos": 25000
            }
            """.trimIndent(),
        )
        assertTrue(lancPresente.estabelecimentoNormalizado.presente)
        assertEquals("PADARIA", lancPresente.estabelecimentoNormalizado.valor)
        assertTrue(lancPresente.beneficio.presente)
        assertNull(lancPresente.beneficio.valor)
        assertTrue(lancPresente.saldoObservadoCentavos.presente)
        assertEquals(25000L, lancPresente.saldoObservadoCentavos.valor)
    }

    @Test
    fun `recarga snapshot preserva beneficio definido`() {
        val recarga = json.decodeFromString(
            RecargaProgramadaVoucherSnapshotDto.serializer(),
            """
            {
              "publicId": "rec-1",
              "instrumentoPublicId": "inst-1",
              "beneficio": "VA",
              "competenciaMes": "2026-11-01",
              "dataPrevista": "2026-11-05",
              "valorPrevistoCentavos": 60000,
              "status": "Pendente",
              "criadoEm": "2026-11-10T12:00:00Z",
              "atualizadoEm": "2026-11-10T12:00:00Z"
            }
            """.trimIndent(),
        )
        assertTrue(recarga.beneficio.presente)
        assertEquals("VA", recarga.beneficio.valor)
    }

    @Test
    fun `payload push F2 serializa centavos e listas esperadas`() {
        val payload = PushChangesPayload(
            configuracoesCartao = listOf(ConfiguracaoCartaoChangeDto("cfg-1", "inst-1", "2026-11-01T00:00:00Z", 5, 12)),
            referenciasSaldoVoucher = listOf(
                ReferenciaSaldoVoucherChangeDto("ref-1", "inst-2", "Abertura", 50000L, null, "2026-11-01T00:00:00Z", null),
            ),
            recargasProgramadasVoucher = listOf(
                RecargaProgramadaVoucherChangeDto("rec-1", "inst-2", "VA", "2026-11-01", "2026-11-05", 60000L, "Pendente", null),
            ),
        )

        val encoded = jsonDeSync.encodeToString(PushChangesPayload.serializer(), payload)
        assertTrue(encoded.contains("configuracoesCartao"))
        assertTrue(encoded.contains("referenciasSaldoVoucher"))
        assertTrue(encoded.contains("recargasProgramadasVoucher"))
        assertTrue(encoded.contains("50000"))
        assertTrue(encoded.contains("60000"))
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
