package com.pingo.core.network.update

import android.content.Intent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class GerenciadorDeAtualizacaoAppTest {
    private val info = AtualizacaoDisponivel(
        versao = "0.5.0",
        tag = "v0.5.0",
        titulo = "Pingo 0.5.0",
        notas = "### Novidades\n- **Logos oficiais**: ícones reais dos bancos.",
        urlPaginaRelease = "https://github.com/oDaviML/pingo-android/releases/tag/v0.5.0",
        urlDownloadApk = "https://github.com/oDaviML/pingo-android/releases/download/v0.5.0/Pingo-0.5.0-release.apk",
        nomeArquivoApk = "Pingo-0.5.0-release.apk",
    )

    @Test
    fun `verificacao em segundo plano respeita intervalo e ignora falha de rede silenciosamente`() = runBlocking {
        val relogio = RelogioMutavel(Instant.parse("2026-11-18T12:00:00Z"))
        var consultas = 0
        var proximoResultado: ResultadoVerificacaoAtualizacao = ResultadoVerificacaoAtualizacao.SemRede
        val gerenciador = GerenciadorDeAtualizacaoApp(
            verificador = object : VerificadorDeAtualizacaoApk {
                override suspend fun verificar(versaoInstalada: String): ResultadoVerificacaoAtualizacao {
                    consultas++
                    return proximoResultado
                }
            },
            baixarApk = { flowOf(EstadoDownloadApk.Concluido(File("Pingo-0.5.0-release.apk"))) },
            podeInstalar = { true },
            abrirPermissaoFontes = { Intent() },
            abrirInstalacaoApk = { Intent() },
            versaoInstalada = "0.4.0",
            codigoVersaoInstalado = 4L,
            clock = relogio,
        )

        gerenciador.verificarEmSegundoPlano()
        assertEquals(EstadoAtualizacaoDoApp.Ocioso, gerenciador.estado.value)
        assertEquals(1, consultas)

        proximoResultado = ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel(info)
        gerenciador.verificarEmSegundoPlano()
        assertEquals(EstadoAtualizacaoDoApp.Disponivel(info), gerenciador.estado.value)
        assertEquals(2, consultas)

        gerenciador.verificarEmSegundoPlano()
        assertEquals(2, consultas)

        relogio.avancar(Duration.ofMinutes(11))
        gerenciador.verificarEmSegundoPlano()
        assertEquals(3, consultas)
    }

    @Test
    fun `verificacao manual expoe falha e download atualiza estado para pronto para instalar`() = runBlocking {
        var podeInstalar = false
        var retorno: ResultadoVerificacaoAtualizacao = ResultadoVerificacaoAtualizacao.SemRede
        val gerenciador = GerenciadorDeAtualizacaoApp(
            verificador = object : VerificadorDeAtualizacaoApk {
                override suspend fun verificar(versaoInstalada: String): ResultadoVerificacaoAtualizacao = retorno
            },
            baixarApk = {
                flowOf(
                    EstadoDownloadApk.EmProgresso(50),
                    EstadoDownloadApk.Concluido(File("Pingo-0.5.0-release.apk")),
                )
            },
            podeInstalar = { podeInstalar },
            abrirPermissaoFontes = { Intent() },
            abrirInstalacaoApk = { Intent() },
            versaoInstalada = "0.4.0",
            codigoVersaoInstalado = 4L,
            clock = Clock.systemUTC(),
        )

        gerenciador.verificarManualmente()
        assertTrue(gerenciador.estado.value is EstadoAtualizacaoDoApp.Falha)

        retorno = ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel(info)
        gerenciador.verificarManualmente()
        assertEquals(EstadoAtualizacaoDoApp.Disponivel(info), gerenciador.estado.value)

        gerenciador.baixar(info)
        val pronto = gerenciador.estado.value as EstadoAtualizacaoDoApp.ProntoParaInstalar
        assertTrue(pronto.precisaPermissaoFontes)

        podeInstalar = true
        gerenciador.atualizarPermissaoInstalacao()
        assertFalse((gerenciador.estado.value as EstadoAtualizacaoDoApp.ProntoParaInstalar).precisaPermissaoFontes)
    }

    private class RelogioMutavel(private var agora: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant(): Instant = agora

        fun avancar(duracao: Duration) {
            agora = agora.plus(duracao)
        }
    }
}
