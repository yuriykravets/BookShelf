package com.partitionsoft.bookshelf.data.subscription.local

import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

interface SubscriptionLocalDataSource {
    fun observeStatus(): Flow<SubscriptionStatus>
    fun observeActivePlanId(): Flow<String?>

    suspend fun updateSubscription(status: SubscriptionStatus, activePlanId: String?)
}
