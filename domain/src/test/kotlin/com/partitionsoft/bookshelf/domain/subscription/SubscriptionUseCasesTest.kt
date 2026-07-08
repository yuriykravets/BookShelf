package com.partitionsoft.bookshelf.domain.subscription

import com.partitionsoft.bookshelf.domain.subscription.model.PremiumFeature
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import com.partitionsoft.bookshelf.domain.subscription.usecase.GetSubscriptionStatusUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.IsFeatureUnlockedUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.IsPremiumUserUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionUseCasesTest {

    @Test
    fun `status use cases expose repository state`() = runTest {
        val repository = FakeSubscriptionRepository(SubscriptionStatus.FREE)

        assertEquals(SubscriptionStatus.FREE, GetSubscriptionStatusUseCase(repository)())
        assertEquals(SubscriptionStatus.FREE, ObserveSubscriptionStatusUseCase(repository)().first())

        repository.status.value = SubscriptionStatus.PREMIUM

        assertEquals(SubscriptionStatus.PREMIUM, GetSubscriptionStatusUseCase(repository)())
        assertEquals(SubscriptionStatus.PREMIUM, ObserveSubscriptionStatusUseCase(repository)().first())
    }

    @Test
    fun `premium check reflects current status`() = runTest {
        val repository = FakeSubscriptionRepository(SubscriptionStatus.FREE)
        val useCase = IsPremiumUserUseCase(repository)

        assertFalse(useCase())
        repository.status.value = SubscriptionStatus.PREMIUM
        assertTrue(useCase())
    }

    @Test
    fun `all premium features are locked for free users`() = runTest {
        val useCase = IsFeatureUnlockedUseCase(
            FakeSubscriptionRepository(SubscriptionStatus.FREE)
        )

        PremiumFeature.entries.forEach { feature ->
            assertFalse("Expected $feature to be locked", useCase(feature))
        }
    }

    @Test
    fun `all premium features are unlocked for premium users`() = runTest {
        val useCase = IsFeatureUnlockedUseCase(
            FakeSubscriptionRepository(SubscriptionStatus.PREMIUM)
        )

        PremiumFeature.entries.forEach { feature ->
            assertTrue("Expected $feature to be unlocked", useCase(feature))
        }
    }

    private class FakeSubscriptionRepository(initialStatus: SubscriptionStatus) : SubscriptionRepository {
        val status = MutableStateFlow(initialStatus)

        override fun observeStatus(): Flow<SubscriptionStatus> = status

        override suspend fun getStatus(): SubscriptionStatus = status.value
    }
}
