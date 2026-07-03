package com.partitionsoft.bookshelf.data.reader.settings

import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.repository.ReaderSettingsRepository
import kotlinx.coroutines.flow.Flow

class LocalReaderSettingsRepository(
    private val localDataSource: ReaderSettingsLocalDataSource
) : ReaderSettingsRepository {
    override fun observeSettings(): Flow<ReaderSettings> = localDataSource.observeSettings()
    override suspend fun updateSettings(settings: ReaderSettings) = localDataSource.updateSettings(settings)
}
