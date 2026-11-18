package com.pingo.core.data.di

import com.pingo.core.data.AccountRepository
import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ConfiguracaoCartaoRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.FaturaRepository
import com.pingo.core.data.GestaoDeCategoriasRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.data.RegraParserRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.UsuarioRepository
import com.pingo.core.data.VoucherRepository
import com.pingo.core.data.assinatura.AgendadorDeAvisosDeAssinatura
import com.pingo.core.data.assinatura.ConfirmadorDeAssinatura
import com.pingo.core.data.captura.ProcessadorDeCaptura
import com.pingo.core.data.impl.AccountRepositoryImpl
import com.pingo.core.data.impl.AssinaturaRepositoryImpl
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ConfiguracaoCartaoRepositoryImpl
import com.pingo.core.data.impl.ConfirmadorDeAssinaturaPorCategoria
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.ContasFinanceiroProjectionImpl
import com.pingo.core.data.impl.CoordenadorDeContaFinanceiroImpl
import com.pingo.core.data.impl.FaturaRepositoryImpl
import com.pingo.core.data.impl.GestaoDeCategoriasRepositoryImpl
import com.pingo.core.data.impl.InicioFinanceiroProjectionImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.ProcessadorDeCapturaImpl
import com.pingo.core.data.impl.ReconciliadorFinanceiroImpl
import com.pingo.core.data.impl.ReconciliadorVoucherCapturaImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RegraParserRepositoryImpl
import com.pingo.core.data.impl.RelatoriosProjectionImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.data.impl.UsuarioRepositoryImpl
import com.pingo.core.data.impl.VoucherRepositoryImpl
import com.pingo.core.data.projecao.ContasFinanceiroProjection
import com.pingo.core.data.projecao.InicioFinanceiroProjection
import com.pingo.core.data.projecao.RelatoriosProjection
import com.pingo.core.data.reconciliacao.CoordenadorDeContaFinanceiro
import com.pingo.core.data.reconciliacao.ReconciliadorFinanceiro
import com.pingo.core.data.reconciliacao.ReconciliadorVoucherCaptura
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindAccountRepository(implementation: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindContaRepository(implementation: ContaRepositoryImpl): ContaRepository

    @Binds
    @Singleton
    abstract fun bindInstrumentoRepository(implementation: InstrumentoRepositoryImpl): InstrumentoRepository

    @Binds
    @Singleton
    abstract fun bindLancamentoRepository(implementation: LancamentoRepositoryImpl): LancamentoRepository

    @Binds
    @Singleton
    abstract fun bindCategoriaRepository(implementation: CategoriaRepositoryImpl): CategoriaRepository

    @Binds
    @Singleton
    abstract fun bindRegraCategoriaRepository(implementation: RegraCategoriaRepositoryImpl): RegraCategoriaRepository

    @Binds
    @Singleton
    abstract fun bindGestaoDeCategoriasRepository(implementation: GestaoDeCategoriasRepositoryImpl): GestaoDeCategoriasRepository

    @Binds
    @Singleton
    abstract fun bindFaturaRepository(implementation: FaturaRepositoryImpl): FaturaRepository

    @Binds
    @Singleton
    abstract fun bindAssinaturaRepository(implementation: AssinaturaRepositoryImpl): AssinaturaRepository

    @Binds
    @Singleton
    abstract fun bindConfirmadorDeAssinatura(implementation: ConfirmadorDeAssinaturaPorCategoria): ConfirmadorDeAssinatura

    @Binds
    @Singleton
    abstract fun bindUsuarioRepository(implementation: UsuarioRepositoryImpl): UsuarioRepository

    @Binds
    @Singleton
    abstract fun bindRegraParserRepository(implementation: RegraParserRepositoryImpl): RegraParserRepository

    @Binds
    @Singleton
    abstract fun bindTransacaoLocal(implementation: RoomTransacaoLocal): TransacaoLocal

    @Binds
    @Singleton
    abstract fun bindProcessadorDeCaptura(implementation: ProcessadorDeCapturaImpl): ProcessadorDeCaptura

    @Binds
    @Singleton
    abstract fun bindConfiguracaoCartaoRepository(implementation: ConfiguracaoCartaoRepositoryImpl): ConfiguracaoCartaoRepository

    @Binds
    @Singleton
    abstract fun bindVoucherRepository(implementation: VoucherRepositoryImpl): VoucherRepository

    @Binds
    @Singleton
    abstract fun bindCoordenadorDeContaFinanceiro(implementation: CoordenadorDeContaFinanceiroImpl): CoordenadorDeContaFinanceiro

    @Binds
    @Singleton
    abstract fun bindReconciliadorFinanceiro(implementation: ReconciliadorFinanceiroImpl): ReconciliadorFinanceiro

    @Binds
    @Singleton
    abstract fun bindReconciliadorVoucherCaptura(implementation: ReconciliadorVoucherCapturaImpl): ReconciliadorVoucherCaptura

    @Binds
    @Singleton
    abstract fun bindContasFinanceiroProjection(implementation: ContasFinanceiroProjectionImpl): ContasFinanceiroProjection

    @Binds
    @Singleton
    abstract fun bindInicioFinanceiroProjection(implementation: InicioFinanceiroProjectionImpl): InicioFinanceiroProjection

    @Binds
    @Singleton
    abstract fun bindRelatoriosProjection(implementation: RelatoriosProjectionImpl): RelatoriosProjection

    @BindsOptionalOf
    abstract fun bindAgendadorDeAvisos(): AgendadorDeAvisosDeAssinatura
}
