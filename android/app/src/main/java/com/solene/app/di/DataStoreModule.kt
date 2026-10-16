package com.solene.app.di

import android.content.Context
import com.solene.app.data.datastore.UserPreferencesRepository
import com.solene.app.data.datastore.soleneDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the DataStore-backed UserPreferencesRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context,
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context.soleneDataStore)
    }
}
