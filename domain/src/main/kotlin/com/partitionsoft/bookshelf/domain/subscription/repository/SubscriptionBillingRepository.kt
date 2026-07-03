package com.partitionsoft.bookshelf.domain.subscription.repository

import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionActionResult
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionBillingAvailability
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPlan
import kotlinx.coroutines.flow.Flow

interface SubscriptionBillingRepository {
    fun observeAvailability(): Flow<SubscriptionBillingAvailability>
    fun observePlans(): Flow<List<SubscriptionPlan>>
    fun observeActivePlanId(): Flow<String?>
    suspend fun purchase(planId: String): SubscriptionActionResult
    suspend fun restore(): SubscriptionActionResult
    suspend fun cancel()
    fun refresh()
}
