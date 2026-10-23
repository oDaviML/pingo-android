package com.pingo

import com.pingo.core.auth.Auth0Config
import com.pingo.core.common.AppDispatchers
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.network.PingoApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers

    @Provides
    @Singleton
    fun provideAuth0Config(): Auth0Config = Auth0Config(
        domain = BuildConfig.AUTH0_DOMAIN,
        clientId = BuildConfig.AUTH0_CLIENT_ID,
        scheme = BuildConfig.AUTH0_SCHEME,
        audience = BuildConfig.AUTH0_AUDIENCE,
    )

    @Provides
    @Singleton
    fun providePingoApiConfig(): PingoApiConfig = PingoApiConfig(baseUrl = BuildConfig.API_BASE_URL)
}
