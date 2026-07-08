package com.partitionsoft.bookshelf.domain.subscription.usecase

import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionBillingRepository

class RefreshSubscriptionPlansUseCase(
    private val repository: SubscriptionBillingRepository
) {
    operator fun invoke() = repository.refresh()
}
