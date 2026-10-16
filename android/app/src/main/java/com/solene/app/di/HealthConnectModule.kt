package com.solene.app.di

import android.content.Context
import com.solene.app.data.healthconnect.HealthConnectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Health Connect repository.
 */
@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule {

    @Provides
    @Singleton
    fun provideHealthConnectRepository(
        @ApplicationContext context: Context,
    ): HealthConnectRepository {
        return HealthConnectRepository(context)
    }
}
