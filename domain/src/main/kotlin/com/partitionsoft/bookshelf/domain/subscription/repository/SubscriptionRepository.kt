package com.partitionsoft.bookshelf.domain.subscription.repository

import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeStatus(): Flow<SubscriptionStatus>

    suspend fun getStatus(): SubscriptionStatus
}
