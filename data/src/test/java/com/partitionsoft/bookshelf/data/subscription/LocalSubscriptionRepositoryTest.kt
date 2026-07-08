package com.partitionsoft.bookshelf.data.subscription

import com.partitionsoft.bookshelf.data.subscription.local.SubscriptionLocalDataSource
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSubscriptionRepositoryTest {

    @Test
    fun `repository exposes local status and subsequent updates`() = runTest {
        val dataSource = FakeSubscriptionLocalDataSource()
        val repository = LocalSubscriptionRepository(dataSource)

        assertEquals(SubscriptionStatus.FREE, repository.getStatus())

        dataSource.updateSubscription(SubscriptionStatus.PREMIUM, "monthly")

        assertEquals(SubscriptionStatus.PREMIUM, repository.getStatus())
        assertEquals(SubscriptionStatus.PREMIUM, repository.observeStatus().first())
    }

    @Test
    fun `local entitlement can be disabled for release builds`() = runTest {
        val dataSource = FakeSubscriptionLocalDataSource()
        dataSource.updateSubscription(SubscriptionStatus.PREMIUM, "monthly")
        val repository = LocalSubscriptionRepository(dataSource, allowLocalEntitlement = false)
        assertEquals(SubscriptionStatus.FREE, repository.getStatus())
    }

    private class FakeSubscriptionLocalDataSource : SubscriptionLocalDataSource {
        private val status = MutableStateFlow(SubscriptionStatus.FREE)
        private val activePlan = MutableStateFlow<String?>(null)

        override fun observeStatus(): Flow<SubscriptionStatus> = status
        override fun observeActivePlanId(): Flow<String?> = activePlan

        override suspend fun updateSubscription(status: SubscriptionStatus, activePlanId: String?) {
            this.status.value = status
            activePlan.value = activePlanId
        }
    }
}
