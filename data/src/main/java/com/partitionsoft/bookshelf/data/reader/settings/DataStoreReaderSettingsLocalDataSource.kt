package com.partitionsoft.bookshelf.data.reader.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontFamily
import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontWeight
import com.partitionsoft.bookshelf.domain.reader.model.ReaderMode
import com.partitionsoft.bookshelf.domain.reader.model.ReaderPageMargin
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSpacing
import com.partitionsoft.bookshelf.domain.reader.model.ReaderTheme
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreReaderSettingsLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : ReaderSettingsLocalDataSource {
    override fun observeSettings(): Flow<ReaderSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            ReaderSettings(
                fontFamily = preferences.enumValue(FONT_FAMILY, ReaderFontFamily.SYSTEM),
                fontSizePercent = preferences[FONT_SIZE] ?: ReaderSettings.DEFAULT_FONT_SIZE_PERCENT,
                fontWeight = preferences.enumValue(FONT_WEIGHT, ReaderFontWeight.NORMAL),
                lineSpacing = preferences.enumValue(LINE_SPACING, ReaderSpacing.NORMAL),
                paragraphSpacing = preferences.enumValue(PARAGRAPH_SPACING, ReaderSpacing.NORMAL),
                pageMargin = preferences.enumValue(PAGE_MARGIN, ReaderPageMargin.NORMAL),
                theme = preferences.enumValue(THEME, ReaderTheme.SYSTEM),
                mode = preferences.enumValue(MODE, ReaderMode.VERTICAL_SCROLL)
            ).normalized()
        }

    override suspend fun updateSettings(settings: ReaderSettings) {
        val normalized = settings.normalized()
        dataStore.edit { preferences ->
            preferences[FONT_FAMILY] = normalized.fontFamily.name
            preferences[FONT_SIZE] = normalized.fontSizePercent
            preferences[FONT_WEIGHT] = normalized.fontWeight.name
            preferences[LINE_SPACING] = normalized.lineSpacing.name
            preferences[PARAGRAPH_SPACING] = normalized.paragraphSpacing.name
            preferences[PAGE_MARGIN] = normalized.pageMargin.name
            preferences[THEME] = normalized.theme.name
            preferences[MODE] = normalized.mode.name
        }
    }

    private inline fun <reified T : Enum<T>> Preferences.enumValue(
        key: Preferences.Key<String>,
        default: T
    ): T = this[key]?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default

    private companion object {
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_SIZE = intPreferencesKey("font_size_percent")
        val FONT_WEIGHT = stringPreferencesKey("font_weight")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val PARAGRAPH_SPACING = stringPreferencesKey("paragraph_spacing")
        val PAGE_MARGIN = stringPreferencesKey("page_margin")
        val THEME = stringPreferencesKey("theme")
        val MODE = stringPreferencesKey("mode")
    }
}
