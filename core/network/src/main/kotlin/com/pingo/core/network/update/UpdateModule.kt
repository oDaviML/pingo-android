package com.pingo.core.network.update

import android.content.Context
import com.pingo.core.common.AppDispatchers
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {
    @Binds
    @Singleton
    abstract fun bindVerificadorDeAtualizacaoApk(implementation: ServicoDeAtualizacaoGitHub): VerificadorDeAtualizacaoApk

    companion object {
        @Provides
        @Singleton
        fun provideConfiguracaoAtualizacaoApk(): ConfiguracaoAtualizacaoApk = ConfiguracaoAtualizacaoApk()

        @Provides
        @Singleton
        fun provideBaixadorDeApk(@ApplicationContext context: Context, dispatchers: AppDispatchers): BaixadorDeApk = BaixadorDeApk(
            diretorioCache = context.cacheDir,
            dispatchers = dispatchers,
        )
    }
}
