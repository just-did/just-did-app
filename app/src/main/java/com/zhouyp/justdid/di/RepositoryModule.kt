package com.zhouyp.justdid.di

import com.zhouyp.justdid.data.repository.DailyReportRepositoryImpl
import com.zhouyp.justdid.data.repository.HomeRepositoryImpl
import com.zhouyp.justdid.data.repository.ConnectionRepositoryImpl
import com.zhouyp.justdid.data.repository.PushRepositoryImpl
import com.zhouyp.justdid.data.repository.SettingsRepositoryImpl
import com.zhouyp.justdid.domain.repository.ConnectionRepository
import com.zhouyp.justdid.domain.repository.DailyReportRepository
import com.zhouyp.justdid.domain.repository.HomeRepository
import com.zhouyp.justdid.domain.repository.PushRepository
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

    @Binds
    abstract fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository

    @Binds
    abstract fun bindPushRepository(impl: PushRepositoryImpl): PushRepository

    @Binds
    abstract fun bindDailyReportRepository(impl: DailyReportRepositoryImpl): DailyReportRepository
}
