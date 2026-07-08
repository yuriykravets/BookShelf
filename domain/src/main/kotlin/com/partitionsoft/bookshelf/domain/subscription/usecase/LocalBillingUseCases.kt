package com.partitionsoft.bookshelf.domain.subscription.usecase

import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionBillingRepository

class ObserveSubscriptionPlansUseCase(private val repository: SubscriptionBillingRepository) {
    operator fun invoke() = repository.observePlans()
}

class ObserveActiveSubscriptionPlanUseCase(private val repository: SubscriptionBillingRepository) {
    operator fun invoke() = repository.observeActivePlanId()
}

class PurchaseSubscriptionUseCase(private val repository: SubscriptionBillingRepository) {
    suspend operator fun invoke(planId: String) = repository.purchase(planId)
}

class RestoreSubscriptionUseCase(private val repository: SubscriptionBillingRepository) {
    suspend operator fun invoke() = repository.restore()
}

class CancelSubscriptionUseCase(private val repository: SubscriptionBillingRepository) {
    suspend operator fun invoke() = repository.cancel()
}
