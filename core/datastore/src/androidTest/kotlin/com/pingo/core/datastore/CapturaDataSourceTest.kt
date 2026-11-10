package com.pingo.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CapturaDataSourceTest {
    private val dataSource = CapturaDataSourceImpl(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun comecaParadoSemRegistro() = runBlocking {
        dataSource.clear()

        val estado = dataSource.estadoListener.first()

        assertEquals(EstadoListener.PARADO, estado.estado)
        assertNull(estado.motivo)
        assertNull(estado.atualizadoEm)
        assertTrue(dataSource.ultimaLeituraPorApp.first().isEmpty())
    }

    @Test
    fun persisteAtivoEParadoComMotivo() = runBlocking {
        dataSource.clear()

        val ativadoEm = Instant.parse("2026-11-10T12:00:00Z")
        val paradoEm = Instant.parse("2026-11-10T13:30:00Z")

        dataSource.registrarAtivo(ativadoEm)

        val ativo = dataSource.estadoListener.first()
        assertEquals(EstadoListener.ATIVO, ativo.estado)
        assertNull(ativo.motivo)
        assertEquals(ativadoEm, ativo.atualizadoEm)

        dataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, paradoEm)

        val parado = dataSource.estadoListener.first()
        assertEquals(EstadoListener.PARADO, parado.estado)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, parado.motivo)
        assertEquals(paradoEm, parado.atualizadoEm)

        dataSource.registrarAtivo(paradoEm)

        assertNull(dataSource.estadoListener.first().motivo)
    }

    @Test
    fun persisteUltimaLeituraPorApp() = runBlocking {
        dataSource.clear()

        val leituraItau = Instant.parse("2026-11-10T12:00:00Z")
        val leituraNubank = Instant.parse("2026-11-10T12:05:00Z")

        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, leituraItau)
        dataSource.registrarLeitura(InstituicaoFinanceira.NUBANK, leituraNubank)

        val leituras = dataSource.ultimaLeituraPorApp.first()

        assertEquals(mapOf(InstituicaoFinanceira.ITAU to leituraItau, InstituicaoFinanceira.NUBANK to leituraNubank), leituras)
    }

    @Test
    fun contaPorMesEDistingueCadaContador() = runBlocking {
        dataSource.clear()

        val novembro = YearMonth.of(2026, 11)
        val outubro = YearMonth.of(2026, 10)

        dataSource.incrementarContador(novembro, ContadorCaptura.IGNORADAS)
        dataSource.incrementarContador(novembro, ContadorCaptura.IGNORADAS)
        dataSource.incrementarContador(novembro, ContadorCaptura.NAO_ENTENDIDAS)
        dataSource.incrementarContador(outubro, ContadorCaptura.PENDENTES)

        val contadoresNovembro = dataSource.contadoresDoMes(novembro).first()
        assertEquals(2, contadoresNovembro.ignoradas)
        assertEquals(1, contadoresNovembro.naoEntendidas)
        assertEquals(0, contadoresNovembro.reconhecidas)
        assertEquals(0, contadoresNovembro.pendentes)

        val contadoresOutubro = dataSource.contadoresDoMes(outubro).first()
        assertEquals(1, contadoresOutubro.pendentes)
        assertEquals(0, contadoresOutubro.ignoradas)
    }

    @Test
    fun limpaTodoOEstadoAoExcluirConta() = runBlocking {
        val mes = YearMonth.of(2026, 11)

        dataSource.registrarAtivo(Instant.parse("2026-11-10T12:00:00Z"))
        dataSource.registrarLeitura(InstituicaoFinanceira.CAJU, Instant.parse("2026-11-10T12:00:00Z"))
        dataSource.incrementarContador(mes, ContadorCaptura.IGNORADAS)

        dataSource.clear()

        assertEquals(EstadoListener.PARADO, dataSource.estadoListener.first().estado)
        assertTrue(dataSource.ultimaLeituraPorApp.first().isEmpty())
        assertEquals(0, dataSource.contadoresDoMes(mes).first().ignoradas)
    }
}
