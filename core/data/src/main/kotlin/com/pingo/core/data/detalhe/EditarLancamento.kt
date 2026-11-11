package com.pingo.core.data.detalhe

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.captura.ConfiancaDaLeitura
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.model.Categoria
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
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
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
) {
    suspend fun editar(edicao: EdicaoDoLancamento): ResultadoDaEdicao {
        val original = lancamentoRepository.buscar(edicao.publicId)?.takeIf { it.excluidoEm == null }
            ?: return ResultadoDaEdicao.NaoEncontrado
        val instrumento = resolverInstrumento(edicao.instrumentoPublicId)
        val destino = resolverInstrumento(edicao.destinoPublicId)
        val categoria = resolverCategoria(edicao.categoriaPublicId)
        val invalidos = validar(edicao, original, instrumento, destino, categoria)

        return when {
            invalidos.isNotEmpty() -> ResultadoDaEdicao.Invalido(invalidos)
            edicao.tipo == TipoLancamento.TRANSFERENCIA -> editarTransferencia(original, edicao, instrumento, destino)
            else -> aplicarEdicao(original, edicao, instrumento, categoria)
        }
    }

    private suspend fun resolverInstrumento(publicId: String?): Instrumento? = publicId
        ?.let { instrumentoRepository.buscar(it) }
        ?.takeIf { it.excluidoEm == null }

    private suspend fun resolverCategoria(publicId: String?): Categoria? = publicId
        ?.let { categoriaRepository.buscar(it) }
        ?.takeIf { it.podeSerEscolhida }

    private fun validar(
        edicao: EdicaoDoLancamento,
        original: Lancamento,
        instrumento: Instrumento?,
        destino: Instrumento?,
        categoria: Categoria?,
    ): Set<CampoDoDetalhe> {
        val doValor = if (edicao.valorCentavos <= 0) setOf(CampoDoDetalhe.VALOR) else emptySet()

        return if (edicao.tipo == TipoLancamento.TRANSFERENCIA) {
            doValor + validarTransferencia(edicao, instrumento, destino)
        } else {
            doValor + validarLancamento(edicao, original, instrumento, categoria)
        }
    }

    private fun validarTransferencia(edicao: EdicaoDoLancamento, instrumento: Instrumento?, destino: Instrumento?): Set<CampoDoDetalhe> =
        buildSet {
            if (edicao.instrumentoPublicId == null || instrumento == null) {
                add(CampoDoDetalhe.INSTRUMENTO)
            }
            if (edicao.destinoPublicId == null || destino == null) {
                add(CampoDoDetalhe.DESTINO)
            } else if (instrumento != null && destino.contaPublicId == instrumento.contaPublicId) {
                add(CampoDoDetalhe.DESTINO)
            }
        }

    private fun validarLancamento(
        edicao: EdicaoDoLancamento,
        original: Lancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
    ): Set<CampoDoDetalhe> = buildSet {
        if (edicao.instrumentoPublicId != null && instrumento == null) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.instrumentoPublicId == null && exigeInstrumento(original)) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.categoriaPublicId != null && categoria == null) {
            add(CampoDoDetalhe.CATEGORIA)
        }
        if (categoria != null && !compativel(categoria, edicao.tipo)) {
            add(CampoDoDetalhe.CATEGORIA)
        }
        if (edicao.aplicarCategoriaATodos && categoria == null) {
            add(CampoDoDetalhe.CATEGORIA)
        }
    }

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
        val lancamento = original.copy(
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
            atualizadoEm = agora,
        )

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
            valorCentavos = edicao.valorCentavos,
            dataHora = edicao.dataHora,
            instrumentoPublicId = destino?.publicId,
            transferenciaId = transferenciaId,
            status = transferenciaResolvida(parceiro.status, parceiro.motivoPendencia),
            motivoPendencia = motivoDaTransferencia(parceiro.status, parceiro.motivoPendencia),
            atualizadoEm = agora,
        ) ?: novoPar(edicao, destino, transferenciaId, agora)

        transacaoLocal.executar {
            lancamentoRepository.salvar(lancamento)
            lancamentoRepository.salvar(par)
        }

        return ResultadoDaEdicao.Salvo(lancamento = lancamento, par = par)
    }

    private fun novoPar(edicao: EdicaoDoLancamento, destino: Instrumento?, transferenciaId: String, agora: Instant): Lancamento =
        Lancamento(
            publicId = UUID.randomUUID().toString(),
            instrumentoPublicId = destino?.publicId,
            transferenciaId = transferenciaId,
            valorCentavos = edicao.valorCentavos,
            dataHora = edicao.dataHora,
            tipo = TipoLancamento.TRANSFERENCIA,
            status = StatusLancamento.CONFIRMADO,
            origem = OrigemLancamento.MANUAL,
            confianca = 1.0,
            criadoEm = agora,
            atualizadoEm = agora,
        )

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
        instrumento == null -> motivoDeInstrumento(original.motivoPendencia)
        original.motivoPendencia == MotivoPendencia.MOEDA_ESTRANGEIRA.name -> MotivoPendencia.MOEDA_ESTRANGEIRA
        original.motivoPendencia == MotivoPendencia.POSSIVEL_DUPLICATA.name -> MotivoPendencia.POSSIVEL_DUPLICATA
        estabelecimento.isNullOrBlank() -> MotivoPendencia.CAMPOS_INCOMPLETOS
        categoria == null -> MotivoPendencia.CATEGORIA_DESCONHECIDA
        else -> null
    }

    private fun motivoDeInstrumento(motivo: String?): MotivoPendencia = if (motivo ==
        MotivoPendencia.INSTRUMENTO_AMBIGUO.name
    ) {
        MotivoPendencia.INSTRUMENTO_AMBIGUO
    } else {
        MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO
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
}
