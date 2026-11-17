package com.pingo.sync

import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.network.sync.InstrumentoChangeDto
import com.pingo.core.network.sync.InstrumentoSnapshotDto
import com.pingo.core.network.sync.LancamentoChangeDto
import com.pingo.core.network.sync.LancamentoSnapshotDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SyncMapperTest {
    @Test
    fun `converte centavos para reais e de volta`() {
        assertEquals(42.9, 4290L.toApiReais(), 0.0001)
        assertEquals(4290L, 42.9.toCentavos())
        assertEquals(0L, 0.0.toCentavos())
        assertEquals(-1250L, (-12.5).toCentavos())
    }

    @Test
    fun `mapeia enums do dominio para os valores da api`() {
        assertEquals("MercadoPago", InstituicaoFinanceira.MERCADO_PAGO.toApiValue())
        assertEquals(InstituicaoFinanceira.MERCADO_PAGO, instituicaoFromApi("MercadoPago"))
        assertEquals("Despesa", TipoLancamento.DESPESA.toApiValue())
        assertEquals(TipoLancamento.TRANSFERENCIA, tipoLancamentoFromApi("Transferencia"))
        assertEquals("Pendente", StatusLancamento.PENDENTE.toApiValue())
        assertEquals(StatusLancamento.PENDENTE, statusLancamentoFromApi("Pendente"))
        assertEquals(InstituicaoFinanceira.OUTRA, instituicaoFromApi("BancoNovo"))
    }

    @Test
    fun `interpreta datas com offset do servidor`() {
        val iso = "2026-10-23T12:34:56.1234567+00:00"

        assertEquals(Instant.parse("2026-10-23T12:34:56.123456700Z"), iso.toInstantValue())
        assertEquals("2026-10-23T12:34:56.123Z", Instant.parse("2026-10-23T12:34:56.123Z").toApiValue())
    }

    @Test
    fun `push de lancamento capturado nao serializa o texto bruto`() {
        val entidade = lancamentoEntity(textoBruto = TEXTO_BRUTO)

        val json = Json.encodeToString(LancamentoChangeDto.serializer(), entidade.toChangeDto())

        assertFalse(json.contains("textoBruto"))
        assertFalse(json.contains(TEXTO_BRUTO))
    }

    @Test
    fun `push de pendente sem instrumento serializa o vazio sem inventar vinculo`() {
        val entidade = lancamentoEntity(textoBruto = null, instrumentoPublicId = null, instituicaoOrigem = InstituicaoFinanceira.INTER)

        val dto = entidade.toChangeDto()
        val json = Json.encodeToString(LancamentoChangeDto.serializer(), dto)

        assertNull(dto.instrumentoPublicId)
        assertEquals("Inter", dto.instituicaoOrigem)
        assertFalse(json.contains("instrumentoPublicId"))
    }

    @Test
    fun `pull de pendente sem instrumento preserva origem estruturada`() {
        val entidade = LancamentoSnapshotDto(
            publicId = "lancamento-1",
            instrumentoPublicId = null,
            valor = 37.90,
            dataHora = "2026-11-10T12:00:00Z",
            tipo = "Despesa",
            status = "Pendente",
            origem = "Notificacao",
            instituicaoOrigem = "MercadoPago",
            confianca = 0.5,
            motivoPendencia = "INSTRUMENTO_NAO_ENCONTRADO",
            criadoEm = "2026-11-10T12:00:00Z",
            atualizadoEm = "2026-11-10T12:00:00Z",
        ).toEntity()

        assertNull(entidade.instrumentoPublicId)
        assertEquals(InstituicaoFinanceira.MERCADO_PAGO, entidade.instituicaoOrigem)
        assertEquals("INSTRUMENTO_NAO_ENCONTRADO", entidade.motivoPendencia)
        assertNull(entidade.textoBruto)
    }

    @Test
    fun `push de instrumento arquivado por cascata leva a procedencia`() {
        val entidade = instrumentoEntity(arquivado = true, arquivadoPorCascata = true)

        val json = Json.encodeToString(InstrumentoChangeDto.serializer(), entidade.toChangeDto())

        assertTrue(json.contains("\"arquivadoPorCascata\":true"))
        assertEquals(true, entidade.toChangeDto().arquivadoPorCascata)
    }

    @Test
    fun `pull de instrumento aplica a procedencia de cascata`() {
        val arquivado = instrumentoSnapshot(arquivado = true, arquivadoPorCascata = true).toEntity()
        val reativado = instrumentoSnapshot(arquivado = false, arquivadoPorCascata = false).toEntity()

        assertTrue(arquivado.arquivado)
        assertTrue(arquivado.arquivadoPorCascata)

        assertFalse(reativado.arquivado)
        assertFalse(reativado.arquivadoPorCascata)
    }

    @Test
    fun `pull de instrumento legado sem procedencia nao inventa cascata`() {
        val json = """
            {"publicId":"instrumento-1","contaPublicId":"conta-1","tipo":"Credito","apelido":"Cartão",
            "arquivado":false,"criadoEm":"2026-11-10T12:00:00Z","atualizadoEm":"2026-11-10T12:00:00Z"}
        """.trimIndent()

        val snapshot = Json.decodeFromString(InstrumentoSnapshotDto.serializer(), json)

        assertNull(snapshot.arquivadoPorCascata)
        assertFalse(snapshot.toEntity().arquivadoPorCascata)
    }

    @Test
    fun `pull de lancamento nao preenche o texto bruto local`() {
        val lancamento = LancamentoSnapshotDto(
            publicId = "lancamento-1",
            instrumentoPublicId = "instrumento-1",
            valor = 37.90,
            dataHora = "2026-11-10T12:00:00Z",
            tipo = "Despesa",
            status = "Confirmado",
            origem = "Notificacao",
            confianca = 1.0,
            criadoEm = "2026-11-10T12:00:00Z",
            atualizadoEm = "2026-11-10T12:00:00Z",
        ).toEntity()

        assertNull(lancamento.textoBruto)
    }

    @Test
    fun `push de transferencia serializa a direcao conhecida e omite a desconhecida`() {
        val entidade = lancamentoEntity(textoBruto = null).copy(
            tipo = TipoLancamento.TRANSFERENCIA,
            transferenciaId = "transferencia-1",
            tipoSemTransferencia = TipoLancamento.RECEITA,
        )

        val json = Json.encodeToString(LancamentoChangeDto.serializer(), entidade.toChangeDto())
        val legado = Json.encodeToString(
            LancamentoChangeDto.serializer(),
            entidade.copy(tipoSemTransferencia = null).toChangeDto(),
        )

        assertTrue(json.contains("\"tipoSemTransferencia\":\"Receita\""))
        assertFalse(legado.contains("tipoSemTransferencia"))
    }

    @Test
    fun `pull interpreta direcao valida e trata valor desconhecido ou ausente como desconhecido`() {
        val valida = snapshot(tipo = "Transferencia", tipoSemTransferencia = "Despesa").toEntity()
        val invalida = snapshot(tipo = "Transferencia", tipoSemTransferencia = "Transferencia").toEntity()
        val ausente = snapshot(tipo = "Transferencia", tipoSemTransferencia = null).toEntity()

        assertEquals(TipoLancamento.DESPESA, valida.tipoSemTransferencia)
        assertNull(invalida.tipoSemTransferencia)
        assertNull(ausente.tipoSemTransferencia)
    }

    private fun instrumentoSnapshot(arquivado: Boolean, arquivadoPorCascata: Boolean?): InstrumentoSnapshotDto = InstrumentoSnapshotDto(
        publicId = "instrumento-1",
        contaPublicId = "conta-1",
        tipo = "Credito",
        apelido = "Cartão",
        ultimosDigitos = "1084",
        arquivado = arquivado,
        arquivadoPorCascata = arquivadoPorCascata,
        fechamentoDia = 5,
        vencimentoDia = 12,
        limiteCredito = 5000.0,
        criadoEm = "2026-11-10T12:00:00Z",
        atualizadoEm = "2026-11-10T12:00:00Z",
    )

    private fun instrumentoEntity(arquivado: Boolean, arquivadoPorCascata: Boolean): InstrumentoEntity = InstrumentoEntity(
        publicId = "instrumento-1",
        contaPublicId = "conta-1",
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão",
        ultimosDigitos = "1084",
        arquivado = arquivado,
        arquivadoPorCascata = arquivadoPorCascata,
        fechamentoDia = 5,
        vencimentoDia = 12,
        beneficio = null,
        saldoCentavos = null,
        diaRecarga = null,
        valorRecargaCentavos = null,
        limiteCreditoCentavos = 500000,
        criadoEm = Instant.parse("2026-11-10T12:00:00Z"),
        atualizadoEm = Instant.parse("2026-11-10T12:00:00Z"),
        excluidoEm = null,
    )

    private fun snapshot(tipo: String, tipoSemTransferencia: String?): LancamentoSnapshotDto = LancamentoSnapshotDto(
        publicId = "lancamento-1",
        instrumentoPublicId = "instrumento-1",
        valor = 50.0,
        dataHora = "2026-11-10T12:00:00Z",
        tipo = tipo,
        tipoSemTransferencia = tipoSemTransferencia,
        transferenciaId = "transferencia-1",
        status = "Confirmado",
        origem = "Manual",
        confianca = 1.0,
        criadoEm = "2026-11-10T12:00:00Z",
        atualizadoEm = "2026-11-10T12:00:00Z",
    )

    private fun lancamentoEntity(
        textoBruto: String?,
        instrumentoPublicId: String? = "instrumento-1",
        instituicaoOrigem: InstituicaoFinanceira? = null,
    ): LancamentoEntity = LancamentoEntity(
        publicId = "lancamento-1",
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = null,
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = null,
        valorCentavos = 3790,
        dataHora = Instant.parse("2026-11-10T12:00:00Z"),
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "CLARO *31987905690",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = instituicaoOrigem,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = "hash-do-lancamento",
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = Instant.parse("2026-11-10T12:00:00Z"),
        atualizadoEm = Instant.parse("2026-11-10T12:00:00Z"),
        excluidoEm = null,
        textoBruto = textoBruto,
    )

    private companion object {
        const val TEXTO_BRUTO = "Olá! Compra aprovada de R$ 37,90 em CLARO *31987905690 no dia 10/11 às 00:55"
    }
}
