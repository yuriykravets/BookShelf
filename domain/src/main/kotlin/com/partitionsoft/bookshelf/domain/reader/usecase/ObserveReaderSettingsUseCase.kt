package com.partitionsoft.bookshelf.domain.reader.usecase

import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.repository.ReaderSettingsRepository
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveReaderSettingsUseCase(
    private val settingsRepository: ReaderSettingsRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    operator fun invoke(): Flow<ReaderSettings> = combine(
        settingsRepository.observeSettings(),
        subscriptionRepository.observeStatus()
    ) { settings, status ->
        ReaderSettingsPolicy.effective(settings, status == SubscriptionStatus.PREMIUM)
    }
}
