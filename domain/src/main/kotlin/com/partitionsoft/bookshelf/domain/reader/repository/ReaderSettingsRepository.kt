package com.partitionsoft.bookshelf.domain.reader.repository

import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import kotlinx.coroutines.flow.Flow

interface ReaderSettingsRepository {
    fun observeSettings(): Flow<ReaderSettings>

    suspend fun updateSettings(settings: ReaderSettings)
}
