package com.partitionsoft.bookshelf.domain.reader.usecase

import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.model.ReaderTheme

internal object ReaderSettingsPolicy {
    fun effective(settings: ReaderSettings, isPremium: Boolean): ReaderSettings {
        val normalized = settings.normalized()
        if (isPremium) return normalized
        return ReaderSettings.Default.copy(
            fontSizePercent = normalized.fontSizePercent,
            theme = normalized.theme.takeIf { it.isFreeTheme() } ?: ReaderTheme.SYSTEM,
            mode = normalized.mode
        )
    }

    fun mergeForStorage(
        requested: ReaderSettings,
        stored: ReaderSettings,
        isPremium: Boolean
    ): ReaderSettings {
        val normalized = requested.normalized()
        if (isPremium) return normalized
        return stored.normalized().copy(
            fontSizePercent = normalized.fontSizePercent,
            theme = normalized.theme.takeIf { it.isFreeTheme() } ?: stored.theme,
            mode = normalized.mode
        )
    }

    private fun ReaderTheme.isFreeTheme(): Boolean =
        this == ReaderTheme.SYSTEM || this == ReaderTheme.LIGHT || this == ReaderTheme.DARK
}
