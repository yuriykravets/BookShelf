package com.partitionsoft.bookshelf.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.partitionsoft.bookshelf.data.reader.settings.DataStoreReaderSettingsLocalDataSource
import com.partitionsoft.bookshelf.data.reader.settings.LocalReaderSettingsRepository
import com.partitionsoft.bookshelf.data.reader.settings.ReaderSettingsLocalDataSource
import com.partitionsoft.bookshelf.domain.reader.repository.ReaderSettingsRepository
import com.partitionsoft.bookshelf.domain.reader.usecase.ObserveReaderSettingsUseCase
import com.partitionsoft.bookshelf.domain.reader.usecase.ResetReaderSettingsUseCase
import com.partitionsoft.bookshelf.domain.reader.usecase.UpdateReaderSettingsUseCase
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReaderSettingsModule {
    @Provides
    @Singleton
    @ReaderPreferences
    fun provideReaderDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("reader_settings.preferences_pb")
        }

    @Provides
    @Singleton
    fun provideReaderSettingsLocalDataSource(
        @ReaderPreferences dataStore: DataStore<Preferences>
    ): ReaderSettingsLocalDataSource = DataStoreReaderSettingsLocalDataSource(dataStore)

    @Provides
    @Singleton
    fun provideReaderSettingsRepository(
        dataSource: ReaderSettingsLocalDataSource
    ): ReaderSettingsRepository = LocalReaderSettingsRepository(dataSource)

    @Provides
    fun provideObserveReaderSettingsUseCase(
        repository: ReaderSettingsRepository,
        subscriptionRepository: SubscriptionRepository
    ) = ObserveReaderSettingsUseCase(repository, subscriptionRepository)

    @Provides
    fun provideUpdateReaderSettingsUseCase(
        repository: ReaderSettingsRepository,
        subscriptionRepository: SubscriptionRepository
    ) = UpdateReaderSettingsUseCase(repository, subscriptionRepository)

    @Provides
    fun provideResetReaderSettingsUseCase(
        repository: ReaderSettingsRepository
    ) = ResetReaderSettingsUseCase(repository)
}
