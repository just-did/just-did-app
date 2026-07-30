package com.zhouyp.justdid.di

import com.zhouyp.justdid.data.repository.HomeRepositoryImpl
import com.zhouyp.justdid.data.repository.SettingsRepositoryImpl
import com.zhouyp.justdid.domain.repository.HomeRepository
import com.zhouyp.justdid.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
