package com.partitionsoft.bookshelf.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookshelf.BuildConfig
import com.partitionsoft.bookshelf.data.subscription.PlayPurchaseLauncher
import com.partitionsoft.bookshelf.data.subscription.PlayPurchaseUpdate
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionActionResult
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionBillingAvailability
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPlan
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.usecase.CancelSubscriptionUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveActiveSubscriptionPlanUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionBillingAvailabilityUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionPlansUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionStatusUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.PurchaseSubscriptionUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.RefreshSubscriptionPlansUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.RestoreSubscriptionUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val billingAvailability: SubscriptionBillingAvailability = SubscriptionBillingAvailability.LOADING,
    val hasError: Boolean = false,
    val plans: List<SubscriptionPlan> = emptyList(),
    val activePlanId: String? = null,
    val isProcessing: Boolean = false,
    val message: PremiumActionMessage? = null
)

enum class PremiumActionMessage {
    PURCHASED,
    PURCHASE_CANCELLED,
    PURCHASE_PENDING,
    ALREADY_OWNED,
    ITEM_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    NETWORK_ERROR,
    ACKNOWLEDGEMENT_FAILED,
    RESTORED,
    NOTHING_TO_RESTORE,
    CANCELLED,
    ERROR
}

private data class BillingActionState(
    val isProcessing: Boolean = false,
    val message: PremiumActionMessage? = null
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    observeSubscriptionStatus: ObserveSubscriptionStatusUseCase,
    observeBillingAvailability: ObserveSubscriptionBillingAvailabilityUseCase,
    observePlans: ObserveSubscriptionPlansUseCase,
    observeActivePlan: ObserveActiveSubscriptionPlanUseCase,
    private val purchaseSubscription: PurchaseSubscriptionUseCase,
    private val restoreSubscription: RestoreSubscriptionUseCase,
    private val cancelSubscription: CancelSubscriptionUseCase,
    private val refreshSubscriptionPlans: RefreshSubscriptionPlansUseCase,
    private val playPurchaseLauncher: Lazy<PlayPurchaseLauncher>
) : ViewModel() {

    private val actionState = MutableStateFlow(BillingActionState())

    init {
        if (!BuildConfig.DEBUG) {
            viewModelScope.launch {
                playPurchaseLauncher.get().observePurchaseUpdates().collect { update ->
                    handlePlayPurchaseUpdate(update)
                }
            }
        }
    }

    val uiState: StateFlow<PremiumUiState> = combine(
        observeSubscriptionStatus(), observeBillingAvailability(), observePlans(), observeActivePlan(), actionState
    ) { status, billingAvailability, plans, activePlanId, action ->
            PremiumUiState(
                isLoading = false,
                isPremium = status == SubscriptionStatus.PREMIUM,
                billingAvailability = billingAvailability,
                plans = plans,
                activePlanId = activePlanId,
                isProcessing = action.isProcessing,
                message = action.message
            )
        }
        .catch {
            emit(PremiumUiState(isLoading = false, hasError = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PremiumUiState()
        )

    fun purchase(planId: String, activity: Activity) = viewModelScope.launch {
        actionState.value = BillingActionState(isProcessing = true)
        if (BuildConfig.DEBUG) {
            val result = purchaseSubscription(planId)
            actionState.value = BillingActionState(
                message = if (result == SubscriptionActionResult.SUCCESS) PremiumActionMessage.PURCHASED else PremiumActionMessage.ERROR
            )
        } else {
            val update = playPurchaseLauncher.get().launchPurchase(activity, planId)
            if (update != PlayPurchaseUpdate.LAUNCHED) {
                actionState.value = BillingActionState(message = update.toActionMessage())
            }
        }
    }

    fun restore() = viewModelScope.launch {
        actionState.value = BillingActionState(isProcessing = true)
        actionState.value = BillingActionState(
            message = when (restoreSubscription()) {
                SubscriptionActionResult.SUCCESS -> PremiumActionMessage.RESTORED
                SubscriptionActionResult.NO_PURCHASE_FOUND -> PremiumActionMessage.NOTHING_TO_RESTORE
                else -> PremiumActionMessage.ERROR
            }
        )
    }

    fun cancel() = viewModelScope.launch {
        actionState.value = BillingActionState(isProcessing = true)
        cancelSubscription()
        actionState.value = BillingActionState(message = PremiumActionMessage.CANCELLED)
    }

    fun consumeMessage() {
        actionState.value = actionState.value.copy(message = null)
    }

    fun retryBilling() = refreshSubscriptionPlans()

    internal fun handlePlayPurchaseUpdate(update: PlayPurchaseUpdate) {
        if (update != PlayPurchaseUpdate.LAUNCHED) {
            actionState.value = BillingActionState(message = update.toActionMessage())
        }
    }

    private fun PlayPurchaseUpdate.toActionMessage(): PremiumActionMessage = when (this) {
        PlayPurchaseUpdate.PURCHASED -> PremiumActionMessage.PURCHASED
        PlayPurchaseUpdate.CANCELED -> PremiumActionMessage.PURCHASE_CANCELLED
        PlayPurchaseUpdate.PENDING -> PremiumActionMessage.PURCHASE_PENDING
        PlayPurchaseUpdate.ALREADY_OWNED -> PremiumActionMessage.ALREADY_OWNED
        PlayPurchaseUpdate.ITEM_UNAVAILABLE -> PremiumActionMessage.ITEM_UNAVAILABLE
        PlayPurchaseUpdate.BILLING_UNAVAILABLE,
        PlayPurchaseUpdate.SERVICE_DISCONNECTED,
        PlayPurchaseUpdate.DEVELOPER_ERROR -> PremiumActionMessage.BILLING_UNAVAILABLE
        PlayPurchaseUpdate.NETWORK_ERROR -> PremiumActionMessage.NETWORK_ERROR
        PlayPurchaseUpdate.ACKNOWLEDGEMENT_FAILED -> PremiumActionMessage.ACKNOWLEDGEMENT_FAILED
        PlayPurchaseUpdate.ERROR,
        PlayPurchaseUpdate.LAUNCHED -> PremiumActionMessage.ERROR
    }
}
