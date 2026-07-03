package com.partitionsoft.bookshelf.data.reader.settings

import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import kotlinx.coroutines.flow.Flow

interface ReaderSettingsLocalDataSource {
    fun observeSettings(): Flow<ReaderSettings>
    suspend fun updateSettings(settings: ReaderSettings)
}
