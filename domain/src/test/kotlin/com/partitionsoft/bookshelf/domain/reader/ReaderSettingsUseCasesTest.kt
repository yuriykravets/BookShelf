package com.partitionsoft.bookshelf.domain.reader

import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontFamily
import com.partitionsoft.bookshelf.domain.reader.model.ReaderMode
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.model.ReaderTheme
import com.partitionsoft.bookshelf.domain.reader.repository.ReaderSettingsRepository
import com.partitionsoft.bookshelf.domain.reader.usecase.ObserveReaderSettingsUseCase
import com.partitionsoft.bookshelf.domain.reader.usecase.UpdateReaderSettingsUseCase
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderSettingsUseCasesTest {
    @Test
    fun `free users keep navigation and basic settings while premium appearance uses defaults`() = runTest {
        val stored = ReaderSettings(
            fontFamily = ReaderFontFamily.SERIF,
            fontSizePercent = 140,
            theme = ReaderTheme.SEPIA,
            mode = ReaderMode.PAGE_TURN
        )
        val settingsRepository = FakeSettingsRepository(stored)
        val subscriptionRepository = FakeSubscriptionRepository(SubscriptionStatus.FREE)

        val effective = ObserveReaderSettingsUseCase(settingsRepository, subscriptionRepository)().first()

        assertEquals(140, effective.fontSizePercent)
        assertEquals(ReaderFontFamily.SYSTEM, effective.fontFamily)
        assertEquals(ReaderTheme.SYSTEM, effective.theme)
        assertEquals(ReaderMode.PAGE_TURN, effective.mode)
    }

    @Test
    fun `premium users receive all stored customization`() = runTest {
        val stored = ReaderSettings(
            fontFamily = ReaderFontFamily.SERIF,
            theme = ReaderTheme.SEPIA,
            mode = ReaderMode.PAGE_TURN
        )
        val settingsRepository = FakeSettingsRepository(stored)

        val effective = ObserveReaderSettingsUseCase(
            settingsRepository,
            FakeSubscriptionRepository(SubscriptionStatus.PREMIUM)
        )().first()

        assertEquals(stored, effective)
    }

    @Test
    fun `free update cannot overwrite premium settings`() = runTest {
        val stored = ReaderSettings(fontFamily = ReaderFontFamily.SERIF, theme = ReaderTheme.SEPIA)
        val settingsRepository = FakeSettingsRepository(stored)
        val useCase = UpdateReaderSettingsUseCase(
            settingsRepository,
            FakeSubscriptionRepository(SubscriptionStatus.FREE)
        )

        useCase(ReaderSettings(fontSizePercent = 999, fontFamily = ReaderFontFamily.MONOSPACE))

        assertEquals(ReaderFontFamily.SERIF, settingsRepository.settings.value.fontFamily)
        assertEquals(ReaderTheme.SYSTEM, settingsRepository.settings.value.theme)
        assertEquals(ReaderSettings.MAX_FONT_SIZE_PERCENT, settingsRepository.settings.value.fontSizePercent)
    }

    private class FakeSettingsRepository(initial: ReaderSettings) : ReaderSettingsRepository {
        val settings = MutableStateFlow(initial)
        override fun observeSettings(): Flow<ReaderSettings> = settings
        override suspend fun updateSettings(settings: ReaderSettings) { this.settings.value = settings }
    }

    private class FakeSubscriptionRepository(initial: SubscriptionStatus) : SubscriptionRepository {
        private val status = MutableStateFlow(initial)
        override fun observeStatus(): Flow<SubscriptionStatus> = status
        override suspend fun getStatus(): SubscriptionStatus = status.value
    }
}
