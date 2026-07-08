package com.partitionsoft.bookshelf.domain.subscription.usecase

import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository

class IsPremiumUserUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(): Boolean = repository.getStatus() == SubscriptionStatus.PREMIUM
}
