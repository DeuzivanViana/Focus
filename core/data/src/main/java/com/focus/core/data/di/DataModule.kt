package com.focus.core.data.di

import com.focus.core.data.repository.DataStoreSettingsRepository
import com.focus.core.data.repository.OfflineSessionRepository
import com.focus.core.data.repository.SessionRepository
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.data.repository.StatsRepository
import com.focus.core.data.repository.DefaultStatsRepository
import com.focus.core.data.repository.DataStoreSelectionRepository
import com.focus.core.data.repository.OfflineSubjectRepository
import com.focus.core.data.repository.SelectionRepository
import com.focus.core.data.repository.SubjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds @Singleton
    abstract fun bindSessionRepository(impl: OfflineSessionRepository): SessionRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds @Singleton
    abstract fun bindSubjectRepository(impl: OfflineSubjectRepository): SubjectRepository

    @Binds @Singleton
    abstract fun bindSelectionRepository(impl: DataStoreSelectionRepository): SelectionRepository

    @Binds @Singleton
    abstract fun bindStatsRepository(impl: DefaultStatsRepository): StatsRepository
}
