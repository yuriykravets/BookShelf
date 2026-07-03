package com.partitionsoft.bookshelf.data.reader.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontFamily
import com.partitionsoft.bookshelf.domain.reader.model.ReaderMode
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.model.ReaderTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreReaderSettingsLocalDataSourceTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `settings default and persist`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { temporaryFolder.newFile("reader.preferences_pb") }
        )
        val source = DataStoreReaderSettingsLocalDataSource(dataStore)
        assertEquals(ReaderSettings.Default, source.observeSettings().first())

        val updated = ReaderSettings(
            fontFamily = ReaderFontFamily.SERIF,
            fontSizePercent = 135,
            theme = ReaderTheme.SEPIA,
            mode = ReaderMode.PAGE_TURN
        )
        source.updateSettings(updated)
        assertEquals(updated, source.observeSettings().first())
    }
}
