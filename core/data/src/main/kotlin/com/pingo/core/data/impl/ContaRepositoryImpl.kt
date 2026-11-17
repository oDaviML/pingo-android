package com.pingo.core.data.impl

import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.ContaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.Instrumento
import com.pingo.core.model.InstrumentoEditado
import com.pingo.core.model.NovaConta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class ContaRepositoryImpl @Inject constructor(
    private val contaDao: ContaDao,
    private val instrumentoDao: InstrumentoDao,
    private val transacao: TransacaoLocal,
) : ContaRepository {
    override fun observeContas(): Flow<List<Conta>> = contaDao.observeAtivas().map { contas -> contas.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Conta? = contaDao.buscar(publicId)?.toModel()

    override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant) {
        require(contas.all(NovaConta::valida)) { "Conta ou instrumento inválido" }

        val criadas = contas.map { it.criar(agora) }

        transacao.executar {
            contaDao.upsertAll(criadas.map { it.conta.toEntity() })
            instrumentoDao.upsertAll(criadas.flatMap { it.instrumentos }.map { it.toEntity() })
        }
    }

    override suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo {
        require(edicao.valida()) { "Conta ou instrumento inválido" }

        return transacao.executar {
            val atual = contaDao.buscar(edicao.publicId)
                ?.takeIf { it.excluidoEm == null }
                ?.toModel()
                ?: throw IllegalArgumentException("Conta inexistente")

            val conta = atual.copy(
                apelido = edicao.apelido.trim(),
                tipo = edicao.tipo,
                atualizadoEm = agora,
            )

            val instrumentos = edicao.instrumentos.map { it.salvar(contaPublicId = conta.publicId, agora = agora) }

            contaDao.upsert(conta.toEntity())
            instrumentoDao.upsertAll(instrumentos.map { it.toEntity() })

            CadastroSalvo(conta = conta, instrumentos = instrumentos)
        }
    }

    override suspend fun salvar(conta: Conta) {
        contaDao.upsert(conta.toEntity())
    }

    override suspend fun salvarTodas(contas: List<Conta>) {
        contaDao.upsertAll(contas.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        contaDao.marcarExcluida(publicId, quando)
    }

    override suspend fun arquivar(publicId: String, quando: Instant) {
        transacao.executar {
            val conta = contaDao.buscar(publicId)?.takeIf { it.excluidoEm == null } ?: return@executar

            if (conta.arquivada) {
                return@executar
            }

            contaDao.definirArquivamento(publicId = publicId, arquivada = true, quando = quando)
            instrumentoDao.arquivarAtivosPorCascata(contaPublicId = publicId, quando = quando)
        }
    }

    override suspend fun reativar(publicId: String, quando: Instant) {
        transacao.executar {
            val conta = contaDao.buscar(publicId)?.takeIf { it.excluidoEm == null } ?: return@executar

            if (!conta.arquivada) {
                return@executar
            }

            contaDao.definirArquivamento(publicId = publicId, arquivada = false, quando = quando)
            instrumentoDao.reativarArquivadosPorCascata(contaPublicId = publicId, quando = quando)
        }
    }

    private suspend fun InstrumentoEditado.salvar(contaPublicId: String, agora: Instant): Instrumento {
        val atual = publicId?.let { id ->
            instrumentoDao.buscar(id)
                ?.takeIf { it.contaPublicId == contaPublicId && it.excluidoEm == null }
                ?.toModel()
                ?: throw IllegalArgumentException("Instrumento não pertence à conta")
        }

        return Instrumento(
            publicId = atual?.publicId ?: publicId ?: UUID.randomUUID().toString(),
            contaPublicId = contaPublicId,
            tipo = tipo,
            apelido = apelido.trim(),
            ultimosDigitos = ultimosDigitos,
            arquivado = atual?.arquivado ?: false,
            arquivadoPorCascata = atual?.arquivadoPorCascata ?: false,
            fechamentoDia = fechamentoDia,
            vencimentoDia = vencimentoDia,
            beneficio = beneficio,
            saldoCentavos = saldoCentavos,
            diaRecarga = diaRecarga,
            valorRecargaCentavos = valorRecargaCentavos,
            limiteCreditoCentavos = atual?.limiteCreditoCentavos,
            criadoEm = atual?.criadoEm ?: agora,
            atualizadoEm = agora,
        )
    }
}
