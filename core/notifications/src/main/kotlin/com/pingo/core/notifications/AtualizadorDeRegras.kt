package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.RegraParserRepository
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.PingoApiException
import com.pingo.core.network.rules.ParserRuleResponse
import com.pingo.core.parser.VersaoRegras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ResultadoAtualizacaoRegras {
    data class Atualizado(val versao: Int) : ResultadoAtualizacaoRegras

    data class SemAlteracao(val versao: Int) : ResultadoAtualizacaoRegras

    data class Falha(val motivo: String) : ResultadoAtualizacaoRegras
}

internal object MotivosDeFalhaDasRegras {
    const val SEM_CONEXAO = "sem_conexao"
    const val REGRAS_INCOMPLETAS = "regras_incompletas"
    const val REGRAS_INVALIDAS = "regras_invalidas"

    fun http(statusCode: Int): String = "http_$statusCode"
}

@Singleton
class AtualizadorDeRegras @Inject constructor(
    private val apiClient: PingoApiClient,
    private val regraParserRepository: RegraParserRepository,
    private val regrasParaCaptura: RegrasParaCaptura,
    private val capturaDataSource: CapturaDataSource,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) {
    private val mutex = Mutex()

    suspend fun atualizar(): ResultadoAtualizacaoRegras = mutex.withLock {
        withContext(dispatchers.io) {
            try {
                aplicar(baixar())
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (erro: Exception) {
                registrarFalha(erro)
            }
        }
    }

    private suspend fun baixar(): List<RegraParser> {
        val agora = clock.instant()
        val regras = apiClient.getParserRules().regras.map { remota -> remota.paraRegra(agora) }

        if (regras.size != INSTITUICOES.size || regras.map(RegraParser::instituicao).toSet() != INSTITUICOES) {
            throw RegrasIncompletasException()
        }

        regras.forEach { regra -> VersaoRegras.compilar(regra) }

        return regras
    }

    private suspend fun aplicar(regras: List<RegraParser>): ResultadoAtualizacaoRegras {
        val versao = regras.maxOf(RegraParser::versao)
        val versoesRemotas = regras.associate { regra -> regra.instituicao to regra.versao }
        val versoesNoCache = regraParserRepository.versoesEmCache()
            .filterKeys { instituicao -> instituicao in INSTITUICOES }

        if (versoesNoCache != versoesRemotas) {
            regraParserRepository.substituirVigentes(regras)
        }

        regrasParaCaptura.atualizar()
        capturaDataSource.registrarRegrasAtualizadas(clock.instant())

        return if (versoesNoCache == versoesRemotas) {
            ResultadoAtualizacaoRegras.SemAlteracao(versao)
        } else {
            ResultadoAtualizacaoRegras.Atualizado(versao)
        }
    }

    private suspend fun registrarFalha(erro: Exception): ResultadoAtualizacaoRegras {
        val motivo = motivoDe(erro)
        capturaDataSource.registrarFalhaAoAtualizarRegras(motivo, clock.instant())

        return ResultadoAtualizacaoRegras.Falha(motivo)
    }

    private fun motivoDe(erro: Exception): String = when (erro) {
        is PingoApiException -> MotivosDeFalhaDasRegras.http(erro.statusCode)
        is RegrasIncompletasException -> MotivosDeFalhaDasRegras.REGRAS_INCOMPLETAS
        is IllegalArgumentException -> MotivosDeFalhaDasRegras.REGRAS_INVALIDAS
        else -> MotivosDeFalhaDasRegras.SEM_CONEXAO
    }

    private fun ParserRuleResponse.paraRegra(agora: Instant): RegraParser = RegraParser(
        publicId = publicId,
        instituicao = instituicaoDe(instituicao),
        versao = versao,
        padroes = padroes.toString(),
        ativa = true,
        publicadaEm = instanteDe(publicadaEm),
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun instituicaoDe(valor: String): InstituicaoFinanceira = when (valor.filter(Char::isLetter).lowercase()) {
        "nubank" -> InstituicaoFinanceira.NUBANK
        "inter" -> InstituicaoFinanceira.INTER
        "itau" -> InstituicaoFinanceira.ITAU
        "mercadopago" -> InstituicaoFinanceira.MERCADO_PAGO
        "caju" -> InstituicaoFinanceira.CAJU
        "outra" -> InstituicaoFinanceira.OUTRA
        else -> throw RegraInvalidaException()
    }

    private fun instanteDe(valor: String): Instant = runCatching { OffsetDateTime.parse(valor).toInstant() }
        .getOrElse { throw RegraInvalidaException() }

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

private class RegrasIncompletasException : IllegalArgumentException()

private class RegraInvalidaException : IllegalArgumentException()
