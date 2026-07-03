package com.partitionsoft.bookshelf.domain.reader.usecase

import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.repository.ReaderSettingsRepository

class ResetReaderSettingsUseCase(
    private val repository: ReaderSettingsRepository
) {
    suspend operator fun invoke() = repository.updateSettings(ReaderSettings.Default)
}
