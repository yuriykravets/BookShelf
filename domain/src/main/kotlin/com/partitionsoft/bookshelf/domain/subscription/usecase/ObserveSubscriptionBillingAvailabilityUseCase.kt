package com.partitionsoft.bookshelf.domain.subscription.usecase

import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionBillingRepository

class ObserveSubscriptionBillingAvailabilityUseCase(
    private val repository: SubscriptionBillingRepository
) {
    operator fun invoke() = repository.observeAvailability()
}
