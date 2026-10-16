package com.solene.app.di

import com.solene.app.data.repository.UserCycleRepository
import com.solene.app.domain.repository.CycleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding the repository interface to its implementation.
 *
 * Uses [UserCycleRepository] with on-device XGBoost predictions — no stock/mock cycles.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCycleRepository(
        impl: UserCycleRepository
    ): CycleRepository
}
