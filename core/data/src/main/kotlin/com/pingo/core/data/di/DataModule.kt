package com.pingo.core.data.di

import com.pingo.core.data.AccountRepository
import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.FaturaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.data.RegraParserRepository
import com.pingo.core.data.UsuarioRepository
import com.pingo.core.data.impl.AccountRepositoryImpl
import com.pingo.core.data.impl.AssinaturaRepositoryImpl
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.FaturaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RegraParserRepositoryImpl
import com.pingo.core.data.impl.UsuarioRepositoryImpl
import dagger.Binds
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
    abstract fun bindFaturaRepository(implementation: FaturaRepositoryImpl): FaturaRepository

    @Binds
    @Singleton
    abstract fun bindAssinaturaRepository(implementation: AssinaturaRepositoryImpl): AssinaturaRepository

    @Binds
    @Singleton
    abstract fun bindUsuarioRepository(implementation: UsuarioRepositoryImpl): UsuarioRepository

    @Binds
    @Singleton
    abstract fun bindRegraParserRepository(implementation: RegraParserRepositoryImpl): RegraParserRepository
}
