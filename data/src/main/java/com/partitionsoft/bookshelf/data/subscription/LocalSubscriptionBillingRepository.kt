package com.partitionsoft.bookshelf.data.subscription

import com.partitionsoft.bookshelf.data.subscription.local.SubscriptionLocalDataSource
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionActionResult
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionBillingAvailability
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPeriod
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPlan
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionBillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first

class LocalSubscriptionBillingRepository(
    private val localDataSource: SubscriptionLocalDataSource
) : SubscriptionBillingRepository {
    override fun observeAvailability(): Flow<SubscriptionBillingAvailability> =
        flowOf(SubscriptionBillingAvailability.AVAILABLE)

    override fun observePlans(): Flow<List<SubscriptionPlan>> = flowOf(PLANS)
    override fun observeActivePlanId(): Flow<String?> = localDataSource.observeActivePlanId()

    override suspend fun purchase(planId: String): SubscriptionActionResult {
        if (PLANS.none { it.id == planId }) return SubscriptionActionResult.PLAN_NOT_FOUND
        localDataSource.updateSubscription(SubscriptionStatus.PREMIUM, planId)
        return SubscriptionActionResult.SUCCESS
    }

    override suspend fun restore(): SubscriptionActionResult =
        if (localDataSource.observeStatus().first() == SubscriptionStatus.PREMIUM) {
            SubscriptionActionResult.SUCCESS
        } else {
            SubscriptionActionResult.NO_PURCHASE_FOUND
        }

    override suspend fun cancel() {
        localDataSource.updateSubscription(SubscriptionStatus.FREE, null)
    }

    override fun refresh() = Unit

    private companion object {
        val PLANS = listOf(
            SubscriptionPlan("inkwell_monthly_local", SubscriptionPeriod.MONTHLY, "$4.99"),
            SubscriptionPlan("inkwell_yearly_local", SubscriptionPeriod.YEARLY, "$39.99", isRecommended = true)
        )
    }
}
