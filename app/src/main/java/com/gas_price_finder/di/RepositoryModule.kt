package com.gas_price_finder.di

import com.gas_price_finder.data.repository.*
import com.gas_price_finder.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindStationRepository(
        impl: StationRepositoryImpl
    ): StationRepository

    @Binds
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    abstract fun bindFavoriteRepository(
        impl: FavoriteRepositoryImpl
    ): FavoriteRepository

    @Binds
    abstract fun bindPriceHistoryRepository(
        impl: PriceHistoryRepositoryImpl
    ): PriceHistoryRepository

    @Binds
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository
}