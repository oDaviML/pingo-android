package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.RegraParserRepository
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.network.ApiConsent
import com.pingo.core.network.ApiDevice
import com.pingo.core.network.ApiProfile
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.PingoApiException
import com.pingo.core.network.rules.ParserRuleResponse
import com.pingo.core.network.rules.ParserRulesResponse
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult
import com.pingo.core.parser.DefaultParserEngine
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.NotificacaoBruta
import com.pingo.core.parser.PadraoRegra
import com.pingo.core.parser.PadroesRegraCodec
import com.pingo.core.parser.RegrasParserSeed
import com.pingo.core.parser.ResultadoParse
import com.pingo.core.parser.TipoLancamentoPadrao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AtualizadorDeRegrasTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC)
    private val capturaDataSource = FakeCapturaDataSource()
    private val dispatchers = object : AppDispatchers {
        override val io = Dispatchers.Unconfined

        override val default = Dispatchers.Unconfined

        override val main = Dispatchers.Unconfined
    }

    @Test
    fun `primeiro download grava as cinco instituicoes e registra a versao`() = runTest {
        val repositorio = FakeRegraParserRepository()
        val regras = RegrasParaCaptura(repositorio)

        val resultado = atualizador(FakePingoApiClient { respostaV1() }, repositorio, regras).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Atualizado(RegrasParserSeed.VERSAO), resultado)
        assertEquals(1, repositorio.substituicoes)
        assertEquals(RegrasParserSeed.VERSAO, repositorio.versoesEmCache().values.maxOrNull())
        assertEquals(
            INSTITUICOES,
            repositorio.observeRegrasAtivas().first().map { it.instituicao }.toSet(),
        )
        assertEquals(clock.instant(), capturaDataSource.estadoDasRegras().atualizadoEm)
        assertNull(capturaDataSource.estadoDasRegras().motivoFalha)
    }

    @Test
    fun `versao nova troca o cache e o parser aplica os padroes novos`() = runTest {
        val repositorio = FakeRegraParserRepository(RegrasParserSeed.regras)
        val regras = RegrasParaCaptura(repositorio)
        val engine = DefaultParserEngine()
        val textoNovo = "Pagamento fora do padrão de R$ 12,34 em LOJA NOVA"

        assertEquals(
            ResultadoParse.NaoEntendida,
            engine.classificar(NotificacaoBruta(texto = textoNovo), regras.versaoDe(InstituicaoFinanceira.ITAU)),
        )

        val resultado = atualizador(FakePingoApiClient { respostaV2() }, repositorio, regras).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Atualizado(RegrasParserSeed.VERSAO + 1), resultado)
        assertEquals(RegrasParserSeed.VERSAO + 1, repositorio.versoesEmCache().values.maxOrNull())
        val transacao = engine.classificar(
            NotificacaoBruta(texto = textoNovo),
            regras.versaoDe(InstituicaoFinanceira.ITAU),
        ) as ResultadoParse.Transacao
        assertEquals(1234L, transacao.dados.valorCentavos)
        assertEquals(TipoLancamentoPadrao.COMPRA, transacao.dados.tipo)
    }

    @Test
    fun `resposta sem as cinco instituicoes nao grava cache parcial`() = runTest {
        val repositorio = FakeRegraParserRepository()
        val regras = RegrasParaCaptura(repositorio)
        val incompletas = respostaV1().let { resposta ->
            resposta.copy(regras = resposta.regras.filterNot { it.instituicao == "CAJU" })
        }

        val resultado = atualizador(FakePingoApiClient { incompletas }, repositorio, regras).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Falha(MotivosDeFalhaDasRegras.REGRAS_INCOMPLETAS), resultado)
        assertEquals(0, repositorio.substituicoes)
        assertEquals(MotivosDeFalhaDasRegras.REGRAS_INCOMPLETAS, capturaDataSource.estadoDasRegras().motivoFalha)
    }

    @Test
    fun `padroes invalidos nao substituem o cache`() = runTest {
        val repositorio = FakeRegraParserRepository(RegrasParserSeed.regras)
        val regras = RegrasParaCaptura(repositorio)
        val invalidas = respostaV1().let { resposta ->
            resposta.copy(
                regras = resposta.regras.map { remota ->
                    if (remota.instituicao == "INTER") remota.copy(padroes = Json.parseToJsonElement("[]").jsonArray) else remota
                },
            )
        }

        val resultado = atualizador(FakePingoApiClient { invalidas }, repositorio, regras).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Falha(MotivosDeFalhaDasRegras.REGRAS_INVALIDAS), resultado)
        assertEquals(0, repositorio.substituicoes)
    }

    @Test
    fun `falha http mantem o cache e a captura continua`() = runTest {
        val repositorio = FakeRegraParserRepository(RegrasParserSeed.regras)
        val regras = RegrasParaCaptura(repositorio)
        val compiladasAntes = regras.versaoDe(InstituicaoFinanceira.ITAU)

        val resultado = atualizador(
            FakePingoApiClient { throw PingoApiException(503, "indisponivel") },
            repositorio,
            regras,
        ).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Falha(MotivosDeFalhaDasRegras.http(503)), resultado)
        assertEquals(0, repositorio.substituicoes)
        assertTrue(compiladasAntes === regras.versaoDe(InstituicaoFinanceira.ITAU))
        assertTrue(engineAindaExtrai(regras))
        assertEquals(clock.instant(), capturaDataSource.estadoDasRegras().falhaEm)
    }

    @Test
    fun `sem rede registra erro sem derrubar a captura`() = runTest {
        val repositorio = FakeRegraParserRepository(RegrasParserSeed.regras)
        val regras = RegrasParaCaptura(repositorio)

        val resultado = atualizador(
            FakePingoApiClient { throw IOException("sem rede") },
            repositorio,
            regras,
        ).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Falha(MotivosDeFalhaDasRegras.SEM_CONEXAO), resultado)
        assertEquals(0, repositorio.substituicoes)
        assertTrue(engineAindaExtrai(regras))
        assertEquals(MotivosDeFalhaDasRegras.SEM_CONEXAO, capturaDataSource.estadoDasRegras().motivoFalha)
    }

    @Test
    fun `download da mesma versao nao reescreve o cache`() = runTest {
        val repositorio = FakeRegraParserRepository()
        val regras = RegrasParaCaptura(repositorio)
        val atualizador = atualizador(FakePingoApiClient { respostaV1() }, repositorio, regras)

        assertEquals(ResultadoAtualizacaoRegras.Atualizado(RegrasParserSeed.VERSAO), atualizador.atualizar())
        assertEquals(ResultadoAtualizacaoRegras.SemAlteracao(RegrasParserSeed.VERSAO), atualizador.atualizar())
        assertEquals(1, repositorio.substituicoes)
    }

    @Test
    fun `versao nova por instituicao atualiza mesmo com a versao maxima igual`() = runTest {
        val proximaVersao = RegrasParserSeed.VERSAO + 1
        val cache = RegrasParserSeed.regras.map { regra ->
            if (regra.instituicao == InstituicaoFinanceira.ITAU) regra.copy(versao = proximaVersao) else regra
        }
        val repositorio = FakeRegraParserRepository(cache)
        val regras = RegrasParaCaptura(repositorio)
        val remoto = respostaComVersao(mapOf(InstituicaoFinanceira.ITAU to proximaVersao, InstituicaoFinanceira.INTER to proximaVersao))

        val resultado = atualizador(FakePingoApiClient { remoto }, repositorio, regras).atualizar()

        assertEquals(ResultadoAtualizacaoRegras.Atualizado(proximaVersao), resultado)
        assertEquals(1, repositorio.substituicoes)
        assertEquals(proximaVersao, repositorio.versoesEmCache()[InstituicaoFinanceira.INTER])
    }

    private suspend fun engineAindaExtrai(regras: RegrasParaCaptura): Boolean {
        val resultado = DefaultParserEngine().classificar(
            NotificacaoBruta(texto = RegrasDeTeste.TEXTO_COMPRA_ITAU),
            regras.versaoDe(InstituicaoFinanceira.ITAU),
        )

        return (resultado as? ResultadoParse.Transacao)?.dados?.valorCentavos == 3790L
    }

    private fun atualizador(api: PingoApiClient, repositorio: RegraParserRepository, regras: RegrasParaCaptura) =
        AtualizadorDeRegras(api, repositorio, regras, capturaDataSource, clock, dispatchers)

    private fun respostaV1() = ParserRulesResponse(
        versao = RegrasParserSeed.VERSAO,
        regras = RegrasParserSeed.regras.map { it.paraResposta() },
    )

    private fun respostaComVersao(versoes: Map<InstituicaoFinanceira, Int>): ParserRulesResponse = ParserRulesResponse(
        versao = versoes.values.max(),
        regras = RegrasParserSeed.regras.map { regra ->
            regra.copy(versao = versoes[regra.instituicao] ?: regra.versao).paraResposta()
        },
    )

    private fun respostaV2(): ParserRulesResponse {
        val novaVersao = RegrasParserSeed.VERSAO + 1
        val regras = RegrasParserSeed.regras.map { regra ->
            if (regra.instituicao != InstituicaoFinanceira.ITAU) {
                regra
            } else {
                regra.copy(
                    publicId = "servidor-itau-v$novaVersao",
                    versao = novaVersao,
                    padroes = PadroesRegraCodec.codificar(
                        PadroesRegraCodec.decodificar(regra.padroes) + PadraoRegra(
                            id = "itau-pagamento-fora-do-padrao",
                            tipoLancamento = TipoLancamentoPadrao.COMPRA,
                            direcao = DirecaoPadrao.DESPESA,
                            ancoras = listOf("Pagamento fora do padrão de R$"),
                            extracoes = mapOf("valor" to "Pagamento fora do padrão de R$ {valor} em"),
                        ),
                    ),
                )
            }
        }

        return ParserRulesResponse(versao = novaVersao, regras = regras.map { it.paraResposta() })
    }

    private fun RegraParser.paraResposta() = ParserRuleResponse(
        publicId = publicId,
        instituicao = instituicao.name,
        versao = versao,
        publicadaEm = "2026-11-10T00:00:00Z",
        padroes = Json.parseToJsonElement(padroes).jsonArray,
    )

    private companion object {
        val INSTITUICOES = setOf(
            InstituicaoFinanceira.NUBANK,
            InstituicaoFinanceira.INTER,
            InstituicaoFinanceira.ITAU,
            InstituicaoFinanceira.MERCADO_PAGO,
            InstituicaoFinanceira.CAJU,
        )
    }
}

private class FakePingoApiClient(private val regras: () -> ParserRulesResponse) : PingoApiClient {
    override suspend fun healthCheck(): Boolean = true

    override suspend fun getProfile(): ApiProfile = error("não utilizado")

    override suspend fun pushChanges(payload: PushChangesPayload): PushChangesResult = error("não utilizado")

    override suspend fun pullChanges(cursor: String?): PullChangesResult = error("não utilizado")

    override suspend fun getParserRules(): ParserRulesResponse = regras()

    override suspend fun activateDevice(deviceId: String, deviceName: String?): ApiDevice = error("não utilizado")

    override suspend fun recordConsent(version: String): ApiConsent = error("não utilizado")

    override suspend fun deleteAccount() = Unit
}
