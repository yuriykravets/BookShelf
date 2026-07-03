package com.partitionsoft.bookshelf.data.subscription

import com.partitionsoft.bookshelf.data.subscription.local.SubscriptionLocalDataSource
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionActionResult
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSubscriptionBillingRepositoryTest {
    @Test
    fun `purchase persists plan and cancel removes entitlement`() = runTest {
        val source = FakeSource()
        val repository = LocalSubscriptionBillingRepository(source)
        val yearly = repository.observePlans().first().first { it.isRecommended }

        assertEquals(SubscriptionActionResult.SUCCESS, repository.purchase(yearly.id))
        assertEquals(SubscriptionStatus.PREMIUM, source.status.value)
        assertEquals(yearly.id, source.activePlan.value)

        repository.cancel()
        assertEquals(SubscriptionStatus.FREE, source.status.value)
        assertEquals(null, source.activePlan.value)
    }

    private class FakeSource : SubscriptionLocalDataSource {
        val status = MutableStateFlow(SubscriptionStatus.FREE)
        val activePlan = MutableStateFlow<String?>(null)
        override fun observeStatus(): Flow<SubscriptionStatus> = status
        override fun observeActivePlanId(): Flow<String?> = activePlan
        override suspend fun updateSubscription(status: SubscriptionStatus, activePlanId: String?) {
            this.status.value = status
            activePlan.value = activePlanId
        }
    }
}
