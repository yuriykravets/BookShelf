package com.partitionsoft.bookshelf.data.subscription

import com.partitionsoft.bookshelf.data.subscription.local.SubscriptionLocalDataSource
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalSubscriptionRepository(
    private val localDataSource: SubscriptionLocalDataSource,
    private val allowLocalEntitlement: Boolean = true
) : SubscriptionRepository {

    override fun observeStatus(): Flow<SubscriptionStatus> = localDataSource.observeStatus()
        .map { status -> if (allowLocalEntitlement) status else SubscriptionStatus.FREE }

    override suspend fun getStatus(): SubscriptionStatus = observeStatus().first()
}
