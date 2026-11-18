package com.pingo.core.data.detalhe

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.assinatura.ConfirmadorDeAssinatura
import com.pingo.core.data.captura.ConfiancaDaLeitura
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.model.Categoria
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import com.pingo.core.model.direcaoDaTransferencia
import com.pingo.core.model.direcaoOposta
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class EditarLancamento @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categoriaAprendida: CategoriaAprendida,
    private val confirmadorDeAssinatura: ConfirmadorDeAssinatura,
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
) {
    constructor(
        lancamentoRepository: LancamentoRepository,
        instrumentoRepository: InstrumentoRepository,
        categoriaRepository: CategoriaRepository,
        categoriaAprendida: CategoriaAprendida,
        transacaoLocal: TransacaoLocal,
        clock: Clock,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        confirmadorDeAssinatura = ConfirmadorDeAssinatura.NoOp,
        transacaoLocal = transacaoLocal,
        clock = clock,
    )
    suspend fun editar(edicao: EdicaoDoLancamento): ResultadoDaEdicao = transacaoLocal.executar {
        editarNaTransacao(edicao)
    }

    private suspend fun editarNaTransacao(edicao: EdicaoDoLancamento): ResultadoDaEdicao {
        val original = lancamentoRepository.buscar(edicao.publicId)?.takeIf { it.excluidoEm == null }
            ?: return ResultadoDaEdicao.NaoEncontrado
        val instrumento = resolverInstrumento(edicao.instrumentoPublicId)
        val destino = resolverInstrumento(edicao.destinoPublicId)
        val instrumentoAtivo = edicao.instrumentoPublicId?.let { instrumentoRepository.buscarAtivo(it) }
        val destinoAtivo = edicao.destinoPublicId?.let { instrumentoRepository.buscarAtivo(it) }
        val categoria = resolverCategoria(edicao.categoriaPublicId)
        val destinoOriginal = destinoOriginalDe(original, edicao.destinoPublicId)
        val invalidos = validar(
            ValidacaoDaEdicao(
                edicao = edicao,
                original = original,
                instrumento = instrumento,
                instrumentoAtivo = instrumentoAtivo,
                destino = destino,
                destinoAtivo = destinoAtivo,
                destinoOriginal = destinoOriginal,
                categoria = categoria,
            ),
        )

        return when {
            invalidos.isNotEmpty() -> ResultadoDaEdicao.Invalido(invalidos)
            edicao.tipo == TipoLancamento.TRANSFERENCIA -> editarTransferencia(original, edicao, instrumento, destino)
            else -> aplicarEdicao(original, edicao, instrumento, categoria)
        }
    }

    private suspend fun resolverInstrumento(publicId: String?): Instrumento? = publicId
        ?.let { instrumentoRepository.buscar(it) }
        ?.takeIf { it.excluidoEm == null }

    private suspend fun destinoOriginalDe(original: Lancamento, destinoPublicId: String?): String? {
        if (destinoPublicId == null) {
            return null
        }

        val transferenciaId = original.transferenciaId ?: return null

        return lancamentoRepository.observeLancamentos().first()
            .firstOrNull { it.transferenciaId == transferenciaId && it.publicId != original.publicId }
            ?.instrumentoPublicId
    }

    private suspend fun resolverCategoria(publicId: String?): Categoria? = publicId
        ?.let { categoriaRepository.buscar(it) }
        ?.takeIf { it.podeSerEscolhida }

    private fun elegivelParaVinculo(escolhido: String, original: String?, ativo: Instrumento?): Boolean =
        ativo != null || escolhido == original

    private fun validar(dados: ValidacaoDaEdicao): Set<CampoDoDetalhe> {
        val doValor = if (dados.edicao.valorCentavos <= 0) setOf(CampoDoDetalhe.VALOR) else emptySet()

        return if (dados.edicao.tipo == TipoLancamento.TRANSFERENCIA) {
            doValor + validarTransferencia(dados)
        } else {
            doValor + validarLancamento(dados)
        }
    }

    private fun validarTransferencia(dados: ValidacaoDaEdicao): Set<CampoDoDetalhe> = buildSet {
        val edicao = dados.edicao
        if (edicao.instrumentoPublicId == null || dados.instrumento == null) {
            add(CampoDoDetalhe.INSTRUMENTO)
        } else if (!elegivelParaVinculo(edicao.instrumentoPublicId, dados.original.instrumentoPublicId, dados.instrumentoAtivo)) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.destinoPublicId == null || dados.destino == null) {
            add(CampoDoDetalhe.DESTINO)
        } else if (dados.instrumento != null && dados.destino.contaPublicId == dados.instrumento.contaPublicId) {
            add(CampoDoDetalhe.DESTINO)
        } else if (!elegivelParaVinculo(edicao.destinoPublicId, dados.destinoOriginal, dados.destinoAtivo)) {
            add(CampoDoDetalhe.DESTINO)
        }
    }

    private fun validarLancamento(dados: ValidacaoDaEdicao): Set<CampoDoDetalhe> = buildSet {
        val edicao = dados.edicao
        if (edicao.instrumentoPublicId != null && dados.instrumento == null) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.instrumentoPublicId != null && dados.instrumento != null) {
            if (!elegivelParaVinculo(edicao.instrumentoPublicId, dados.original.instrumentoPublicId, dados.instrumentoAtivo)) {
                add(CampoDoDetalhe.INSTRUMENTO)
            }
        }
        if (edicao.instrumentoPublicId == null && exigeInstrumento(dados.original)) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.categoriaPublicId != null && dados.categoria == null) {
            add(CampoDoDetalhe.CATEGORIA)
        }
        if (dados.categoria != null && !compativel(dados.categoria, edicao.tipo)) {
            add(CampoDoDetalhe.CATEGORIA)
        }
        if (edicao.aplicarCategoriaATodos && dados.categoria == null) {
            add(CampoDoDetalhe.CATEGORIA)
        }
    }

    private data class ValidacaoDaEdicao(
        val edicao: EdicaoDoLancamento,
        val original: Lancamento,
        val instrumento: Instrumento?,
        val instrumentoAtivo: Instrumento?,
        val destino: Instrumento?,
        val destinoAtivo: Instrumento?,
        val destinoOriginal: String?,
        val categoria: Categoria?,
    )

    private fun exigeInstrumento(original: Lancamento): Boolean =
        original.origem == OrigemLancamento.MANUAL || original.status == StatusLancamento.CONFIRMADO

    private fun compativel(categoria: Categoria, tipo: TipoLancamento): Boolean = when (tipo) {
        TipoLancamento.DESPESA -> categoria.tipo == TipoCategoria.DESPESA
        TipoLancamento.RECEITA -> categoria.tipo == TipoCategoria.RECEITA
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> true
    }

    private suspend fun aplicarEdicao(
        original: Lancamento,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val estabelecimento = edicao.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val motivo = motivoReavaliado(original, instrumento, categoria, estabelecimento)
        val beneficioAtualizado = if (instrumento?.tipo == TipoInstrumento.VOUCHER && original.beneficio == null) {
            instrumento.beneficio
        } else {
            original.beneficio
        }
        val baseAtualizada = original.copy(
            tipo = edicao.tipo,
            valorCentavos = edicao.valorCentavos,
            instrumentoPublicId = instrumento?.publicId,
            categoriaPublicId = categoria?.publicId,
            dataHora = edicao.dataHora,
            estabelecimento = estabelecimento,
            status = statusDe(original.status, motivo),
            motivoPendencia = motivo?.name,
            confianca = confiancaDe(original, edicao.valorCentavos, estabelecimento, categoria),
            transferenciaId = null,
            tipoSemTransferencia = null,
            atualizadoEm = agora,
            estabelecimentoNormalizado = NormalizadorDeAssinatura.normalizar(estabelecimento)?.take(LIMITE_NORMALIZADO),
            beneficio = beneficioAtualizado,
        )
        val lancamento = confirmadorDeAssinatura.aoEditarLancamento(original, baseAtualizada, categoria)

        lancamentoRepository.salvar(lancamento)
        aprender(edicao, estabelecimento, categoria, agora)

        return ResultadoDaEdicao.Salvo(lancamento)
    }

    private suspend fun editarTransferencia(
        original: Lancamento,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        destino: Instrumento?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val transferenciaId = original.transferenciaId ?: UUID.randomUUID().toString()
        val parceiro = lancamentoRepository.observeLancamentos().first()
            .firstOrNull { it.transferenciaId == transferenciaId && it.publicId != original.publicId }
        val lancamento = original.copy(
            tipo = TipoLancamento.TRANSFERENCIA,
            tipoSemTransferencia = original.direcaoDaTransferencia(),
            valorCentavos = edicao.valorCentavos,
            dataHora = edicao.dataHora,
            instrumentoPublicId = instrumento?.publicId,
            transferenciaId = transferenciaId,
            status = transferenciaResolvida(original.status, original.motivoPendencia),
            motivoPendencia = motivoDaTransferencia(original.status, original.motivoPendencia),
            atualizadoEm = agora,
        )
        val par = parceiro?.copy(
            tipo = TipoLancamento.TRANSFERENCIA,
            tipoSemTransferencia = parceiro.direcaoDaTransferencia(),
            valorCentavos = edicao.valorCentavos,
            dataHora = edicao.dataHora,
            instrumentoPublicId = destino?.publicId,
            transferenciaId = transferenciaId,
            status = transferenciaResolvida(parceiro.status, parceiro.motivoPendencia),
            motivoPendencia = motivoDaTransferencia(parceiro.status, parceiro.motivoPendencia),
            atualizadoEm = agora,
        ) ?: novoPar(edicao, destino, transferenciaId, original.direcaoDaTransferencia()?.direcaoOposta, agora)

        lancamentoRepository.salvar(lancamento)
        lancamentoRepository.salvar(par)

        return ResultadoDaEdicao.Salvo(lancamento = lancamento, par = par)
    }

    private fun novoPar(
        edicao: EdicaoDoLancamento,
        destino: Instrumento?,
        transferenciaId: String,
        direcao: TipoLancamento?,
        agora: Instant,
    ): Lancamento = Lancamento(
        publicId = UUID.randomUUID().toString(),
        instrumentoPublicId = destino?.publicId,
        transferenciaId = transferenciaId,
        valorCentavos = edicao.valorCentavos,
        dataHora = edicao.dataHora,
        tipo = TipoLancamento.TRANSFERENCIA,
        tipoSemTransferencia = direcao,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun Lancamento.direcaoDaTransferencia(): TipoLancamento? = tipoSemTransferencia ?: tipo.direcaoDaTransferencia

    private fun transferenciaResolvida(status: StatusLancamento, motivo: String?): StatusLancamento =
        if (status == StatusLancamento.PENDENTE && motivo == MotivoPendencia.TRANSFERENCIA_AMBIGUA.name) {
            StatusLancamento.CONFIRMADO
        } else {
            status
        }

    private fun motivoDaTransferencia(status: StatusLancamento, motivo: String?): String? =
        if (status == StatusLancamento.PENDENTE && motivo == MotivoPendencia.TRANSFERENCIA_AMBIGUA.name) null else motivo

    private fun motivoReavaliado(
        original: Lancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
        estabelecimento: String?,
    ): MotivoPendencia? = when {
        original.status == StatusLancamento.CONFIRMADO -> null
        original.motivoPendencia == MotivoPendencia.DIVERGENCIA_RECARGA.name -> MotivoPendencia.DIVERGENCIA_RECARGA
        instrumento == null -> motivoDeInstrumento(original.motivoPendencia)
        original.motivoPendencia == MotivoPendencia.MOEDA_ESTRANGEIRA.name -> MotivoPendencia.MOEDA_ESTRANGEIRA
        original.motivoPendencia == MotivoPendencia.POSSIVEL_DUPLICATA.name -> MotivoPendencia.POSSIVEL_DUPLICATA
        estabelecimento.isNullOrBlank() -> MotivoPendencia.CAMPOS_INCOMPLETOS
        categoria == null -> MotivoPendencia.CATEGORIA_DESCONHECIDA
        else -> null
    }

    private fun motivoDeInstrumento(motivo: String?): MotivoPendencia = when (motivo) {
        MotivoPendencia.INSTRUMENTO_AMBIGUO.name -> MotivoPendencia.INSTRUMENTO_AMBIGUO
        MotivoPendencia.BENEFICIO_AMBIGUO.name -> MotivoPendencia.BENEFICIO_AMBIGUO
        else -> MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO
    }

    private fun statusDe(original: StatusLancamento, motivo: MotivoPendencia?): StatusLancamento =
        if (original == StatusLancamento.CONFIRMADO || motivo == null) StatusLancamento.CONFIRMADO else StatusLancamento.PENDENTE

    private fun confiancaDe(original: Lancamento, valorCentavos: Long, estabelecimento: String?, categoria: Categoria?): Double =
        if (original.origem == OrigemLancamento.NOTIFICACAO) {
            ConfiancaDaLeitura.calcular(valorCentavos, estabelecimento, categoria)
        } else {
            original.confianca
        }

    private suspend fun aprender(edicao: EdicaoDoLancamento, estabelecimento: String?, categoria: Categoria?, agora: Instant) {
        if (!edicao.aplicarCategoriaATodos || estabelecimento == null || categoria == null) {
            return
        }

        categoriaAprendida.aprender(estabelecimento, categoria.publicId, agora)
    }

    private companion object {
        const val LIMITE_NORMALIZADO = 200
    }
}
