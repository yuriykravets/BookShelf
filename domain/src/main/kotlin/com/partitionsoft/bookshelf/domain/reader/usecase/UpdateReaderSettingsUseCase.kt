package com.partitionsoft.bookshelf.domain.reader.usecase

import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.repository.ReaderSettingsRepository
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first

class UpdateReaderSettingsUseCase(
    private val settingsRepository: ReaderSettingsRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(settings: ReaderSettings) {
        val stored = settingsRepository.observeSettings().first()
        val isPremium = subscriptionRepository.getStatus() == SubscriptionStatus.PREMIUM
        settingsRepository.updateSettings(
            ReaderSettingsPolicy.mergeForStorage(settings, stored, isPremium)
        )
    }
}
