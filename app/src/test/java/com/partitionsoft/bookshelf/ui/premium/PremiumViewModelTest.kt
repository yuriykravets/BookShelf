package com.partitionsoft.bookshelf.ui.premium

import android.app.Activity
import com.partitionsoft.bookshelf.data.subscription.PlayPurchaseLauncher
import com.partitionsoft.bookshelf.data.subscription.PlayPurchaseUpdate
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionStatusUseCase
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionActionResult
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionBillingAvailability
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPlan
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionBillingRepository
import com.partitionsoft.bookshelf.domain.subscription.usecase.CancelSubscriptionUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveActiveSubscriptionPlanUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionPlansUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionBillingAvailabilityUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.RefreshSubscriptionPlansUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.PurchaseSubscriptionUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.RestoreSubscriptionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import dagger.Lazy

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {

    @Test
    fun `view model reflects subscription changes`() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val repository = FakeSubscriptionRepository()
                val billingRepository = FakeBillingRepository()
                val viewModel = PremiumViewModel(
                    ObserveSubscriptionStatusUseCase(repository),
                    ObserveSubscriptionBillingAvailabilityUseCase(billingRepository),
                    ObserveSubscriptionPlansUseCase(billingRepository),
                    ObserveActiveSubscriptionPlanUseCase(billingRepository),
                    PurchaseSubscriptionUseCase(billingRepository),
                    RestoreSubscriptionUseCase(billingRepository),
                    CancelSubscriptionUseCase(billingRepository),
                    RefreshSubscriptionPlansUseCase(billingRepository),
                    Lazy { FakePlayPurchaseLauncher() }
                )
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

                advanceUntilIdle()
                assertFalse(viewModel.uiState.value.isLoading)
                assertFalse(viewModel.uiState.value.isPremium)

                repository.status.value = SubscriptionStatus.PREMIUM
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.isPremium)
                assertEquals(false, viewModel.uiState.value.hasError)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `every terminal Play update clears processing and exposes useful message`() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val viewModel = createViewModel()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

                val expectations = mapOf(
                    PlayPurchaseUpdate.PURCHASED to PremiumActionMessage.PURCHASED,
                    PlayPurchaseUpdate.CANCELED to PremiumActionMessage.PURCHASE_CANCELLED,
                    PlayPurchaseUpdate.PENDING to PremiumActionMessage.PURCHASE_PENDING,
                    PlayPurchaseUpdate.ALREADY_OWNED to PremiumActionMessage.ALREADY_OWNED,
                    PlayPurchaseUpdate.ITEM_UNAVAILABLE to PremiumActionMessage.ITEM_UNAVAILABLE,
                    PlayPurchaseUpdate.BILLING_UNAVAILABLE to PremiumActionMessage.BILLING_UNAVAILABLE,
                    PlayPurchaseUpdate.NETWORK_ERROR to PremiumActionMessage.NETWORK_ERROR,
                    PlayPurchaseUpdate.SERVICE_DISCONNECTED to PremiumActionMessage.BILLING_UNAVAILABLE,
                    PlayPurchaseUpdate.DEVELOPER_ERROR to PremiumActionMessage.BILLING_UNAVAILABLE,
                    PlayPurchaseUpdate.ACKNOWLEDGEMENT_FAILED to PremiumActionMessage.ACKNOWLEDGEMENT_FAILED,
                    PlayPurchaseUpdate.ERROR to PremiumActionMessage.ERROR
                )

                expectations.forEach { (update, message) ->
                    viewModel.handlePlayPurchaseUpdate(update)
                    advanceUntilIdle()
                    assertFalse(viewModel.uiState.value.isProcessing)
                    assertEquals(message, viewModel.uiState.value.message)
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(): PremiumViewModel {
        val subscriptionRepository = FakeSubscriptionRepository()
        val billingRepository = FakeBillingRepository()
        return PremiumViewModel(
            ObserveSubscriptionStatusUseCase(subscriptionRepository),
            ObserveSubscriptionBillingAvailabilityUseCase(billingRepository),
            ObserveSubscriptionPlansUseCase(billingRepository),
            ObserveActiveSubscriptionPlanUseCase(billingRepository),
            PurchaseSubscriptionUseCase(billingRepository),
            RestoreSubscriptionUseCase(billingRepository),
            CancelSubscriptionUseCase(billingRepository),
            RefreshSubscriptionPlansUseCase(billingRepository),
            Lazy { FakePlayPurchaseLauncher() }
        )
    }

    private class FakeSubscriptionRepository : SubscriptionRepository {
        val status = MutableStateFlow(SubscriptionStatus.FREE)

        override fun observeStatus(): Flow<SubscriptionStatus> = status

        override suspend fun getStatus(): SubscriptionStatus = status.value
    }

    private class FakeBillingRepository : SubscriptionBillingRepository {
        override fun observeAvailability(): Flow<SubscriptionBillingAvailability> =
            MutableStateFlow(SubscriptionBillingAvailability.AVAILABLE)
        override fun observePlans(): Flow<List<SubscriptionPlan>> = MutableStateFlow(emptyList())
        override fun observeActivePlanId(): Flow<String?> = MutableStateFlow(null)
        override suspend fun purchase(planId: String) = SubscriptionActionResult.SUCCESS
        override suspend fun restore() = SubscriptionActionResult.NO_PURCHASE_FOUND
        override suspend fun cancel() = Unit
        override fun refresh() = Unit
    }

    private class FakePlayPurchaseLauncher : PlayPurchaseLauncher {
        private val updates = MutableSharedFlow<PlayPurchaseUpdate>()
        override fun launchPurchase(activity: Activity, planId: String) = PlayPurchaseUpdate.LAUNCHED
        override fun observePurchaseUpdates(): Flow<PlayPurchaseUpdate> = updates
    }
}
