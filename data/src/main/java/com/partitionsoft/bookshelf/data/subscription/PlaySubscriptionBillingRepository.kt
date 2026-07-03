package com.partitionsoft.bookshelf.data.subscription

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionActionResult
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionBillingAvailability
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPeriod
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPlan
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionBillingRepository
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlaySubscriptionBillingRepository(
    context: Context,
    private val productId: String
) : SubscriptionRepository, SubscriptionBillingRepository, PlayPurchaseLauncher, PurchasesUpdatedListener {

    private val status = MutableStateFlow(SubscriptionStatus.FREE)
    private val availability = MutableStateFlow(SubscriptionBillingAvailability.LOADING)
    private val plans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    private val activePlanId = MutableStateFlow<String?>(null)
    private val purchaseUpdates = MutableSharedFlow<PlayPurchaseUpdate>(extraBufferCapacity = 8)
    private val purchaseOptions = mutableMapOf<String, PurchaseOption>()
    private var pendingPlanId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var isBillingConnected = false
    private var isConnectionInProgress = false

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        Log.i(TAG, "Test product=$productId")
        connect()
    }

    override fun observeAvailability(): Flow<SubscriptionBillingAvailability> = availability
    override fun observeStatus(): Flow<SubscriptionStatus> = status
    override suspend fun getStatus(): SubscriptionStatus = status.first()
    override fun observePlans(): Flow<List<SubscriptionPlan>> = plans
    override fun observeActivePlanId(): Flow<String?> = activePlanId

    override suspend fun purchase(planId: String): SubscriptionActionResult =
        if (purchaseOptions.containsKey(planId)) {
            SubscriptionActionResult.SUCCESS
        } else {
            SubscriptionActionResult.PLAN_NOT_FOUND
        }

    override fun observePurchaseUpdates(): Flow<PlayPurchaseUpdate> = purchaseUpdates

    override fun launchPurchase(activity: Activity, planId: String): PlayPurchaseUpdate {
        val option = purchaseOptions[planId] ?: return PlayPurchaseUpdate.ITEM_UNAVAILABLE
        Log.i(TAG, "Launching Google Play purchase flow for basePlan=$planId")
        pendingPlanId = planId
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(option.productDetails)
            .setOfferToken(option.offerToken)
            .build()
        val result = runCatching {
            billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build()
            )
        }.getOrElse { error ->
            pendingPlanId = null
            Log.e(TAG, "Could not launch Google Play purchase flow", error)
            return PlayPurchaseUpdate.ERROR
        }
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            PlayPurchaseUpdate.LAUNCHED
        } else {
            pendingPlanId = null
            result.toPurchaseUpdate()
        }
    }

    override suspend fun restore(): SubscriptionActionResult {
        if (!isBillingConnected) return SubscriptionActionResult.NO_PURCHASE_FOUND
        val purchases = queryPurchases()
        processPurchases(purchases)
        return if (status.value == SubscriptionStatus.PREMIUM) {
            SubscriptionActionResult.SUCCESS
        } else {
            SubscriptionActionResult.NO_PURCHASE_FOUND
        }
    }

    override suspend fun cancel() = Unit

    override fun refresh() {
        Log.i(TAG, "Refreshing Google Play subscription details")
        availability.value = SubscriptionBillingAvailability.LOADING
        plans.value = emptyList()
        purchaseOptions.clear()
        if (isBillingConnected) {
            queryPlans()
            queryExistingPurchases()
        } else {
            connect()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val matching = purchases.orEmpty().filter { it.products.contains(productId) }
                processPurchases(matching)
                when {
                    matching.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } ->
                        purchaseUpdates.tryEmit(PlayPurchaseUpdate.PURCHASED)
                    matching.any { it.purchaseState == Purchase.PurchaseState.PENDING } ->
                        purchaseUpdates.tryEmit(PlayPurchaseUpdate.PENDING)
                    else -> purchaseUpdates.tryEmit(PlayPurchaseUpdate.ERROR)
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                pendingPlanId = null
                queryExistingPurchases()
                purchaseUpdates.tryEmit(PlayPurchaseUpdate.ALREADY_OWNED)
            }
            else -> {
                pendingPlanId = null
                purchaseUpdates.tryEmit(billingResult.toPurchaseUpdate())
            }
        }
    }

    private fun connect() {
        Log.i(
            TAG,
            "Preparing Google Play Billing connection; " +
                "connected=$isBillingConnected, connecting=$isConnectionInProgress"
        )
        if (isBillingConnected) {
            Log.i(TAG, "Google Play Billing is connected; querying subscriptions")
            queryPlans()
            queryExistingPurchases()
            return
        }
        if (isConnectionInProgress) {
            Log.i(TAG, "Google Play Billing connection is already in progress")
            return
        }
        isConnectionInProgress = true
        Log.i(TAG, "Starting Google Play Billing connection")
        armTimeout("Billing connection")
        runCatching {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    isConnectionInProgress = false
                    isBillingConnected = result.responseCode == BillingClient.BillingResponseCode.OK
                    Log.i(
                        TAG,
                        "Billing setup finished: responseCode=${result.responseCode}, " +
                            "message=${result.debugMessage}"
                    )
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryPlans()
                        queryExistingPurchases()
                    } else {
                        reportUnavailable("Billing setup", result)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnectionInProgress = false
                    isBillingConnected = false
                    cancelTimeout()
                    availability.value = SubscriptionBillingAvailability.UNAVAILABLE
                    Log.w(TAG, "Google Play Billing service disconnected")
                }
            })
        }.onFailure { error ->
            isConnectionInProgress = false
            isBillingConnected = false
            cancelTimeout()
            availability.value = SubscriptionBillingAvailability.UNAVAILABLE
            Log.e(TAG, "Could not start Google Play Billing connection", error)
        }
    }

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            Log.i(
                TAG,
                "Existing purchases query finished: responseCode=${billingResult.responseCode}, " +
                    "purchases=${purchases.size}"
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun queryPlans() {
        Log.i(TAG, "Querying subscription product=$productId")
        armTimeout("Subscription query")
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        ) { result, detailsResult ->
            cancelTimeout()
            Log.i(
                TAG,
                "Product query finished: responseCode=${result.responseCode}, " +
                    "fetched=${detailsResult.productDetailsList.size}, " +
                    "unfetched=${detailsResult.unfetchedProductList.size}, " +
                    "message=${result.debugMessage}"
            )
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                reportUnavailable("Product query", result)
                return@queryProductDetailsAsync
            }
            val mapped = detailsResult.productDetailsList
                .flatMap { details ->
                    details.subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
                        val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
                        val period = phase.billingPeriod.toSubscriptionPeriod() ?: return@mapNotNull null
                        val planId = offer.basePlanId
                        purchaseOptions[planId] = PurchaseOption(details, offer.offerToken)
                        SubscriptionPlan(
                            id = planId,
                            period = period,
                            formattedPrice = phase.formattedPrice,
                            isRecommended = period == SubscriptionPeriod.YEARLY
                        )
                    }
                }
                .distinctBy { it.id }
                .sortedBy { it.period.ordinal }
            plans.value = mapped
            availability.value = if (mapped.isNotEmpty()) {
                Log.i(TAG, "Loaded ${mapped.size} eligible subscription plan(s)")
                SubscriptionBillingAvailability.AVAILABLE
            } else {
                Log.w(
                    TAG,
                    "No eligible subscription offers returned for product=$productId; " +
                        "unfetched=${detailsResult.unfetchedProductList}"
                )
                SubscriptionBillingAvailability.UNAVAILABLE
            }
        }
    }

    private fun reportUnavailable(operation: String, result: BillingResult) {
        cancelTimeout()
        availability.value = SubscriptionBillingAvailability.UNAVAILABLE
        Log.w(
            TAG,
            "$operation failed: responseCode=${result.responseCode}, message=${result.debugMessage}"
        )
    }

    private fun armTimeout(operation: String) {
        cancelTimeout()
        timeoutRunnable = Runnable {
            if (availability.value == SubscriptionBillingAvailability.LOADING) {
                isConnectionInProgress = false
                availability.value = SubscriptionBillingAvailability.UNAVAILABLE
                Log.w(TAG, "$operation timed out after ${BILLING_TIMEOUT_MS}ms")
            }
        }.also { mainHandler.postDelayed(it, BILLING_TIMEOUT_MS) }
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let(mainHandler::removeCallbacks)
        timeoutRunnable = null
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val activePurchases = purchases.filter { purchase ->
            purchase.products.contains(productId) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        status.value = if (activePurchases.isNotEmpty()) SubscriptionStatus.PREMIUM else SubscriptionStatus.FREE
        if (activePurchases.isNotEmpty()) {
            activePlanId.value = pendingPlanId ?: activePlanId.value
            pendingPlanId = null
        } else {
            activePlanId.value = null
        }
        activePurchases.filterNot { it.isAcknowledged }.forEach { purchase ->
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            ) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Purchase acknowledgement failed: ${result.debugMessage}")
                    purchaseUpdates.tryEmit(PlayPurchaseUpdate.ACKNOWLEDGEMENT_FAILED)
                }
            }
        }
    }

    private suspend fun queryPurchases(): List<Purchase> = suspendCancellableCoroutine { continuation ->
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            continuation.resume(
                if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases else emptyList()
            )
        }
    }

    private fun String.toSubscriptionPeriod(): SubscriptionPeriod? = when (this) {
        "P1M" -> SubscriptionPeriod.MONTHLY
        "P1Y" -> SubscriptionPeriod.YEARLY
        else -> null
    }

    private fun BillingResult.toPurchaseUpdate(): PlayPurchaseUpdate = when (responseCode) {
        BillingClient.BillingResponseCode.USER_CANCELED -> PlayPurchaseUpdate.CANCELED
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PlayPurchaseUpdate.ALREADY_OWNED
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> PlayPurchaseUpdate.ITEM_UNAVAILABLE
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> PlayPurchaseUpdate.BILLING_UNAVAILABLE
        BillingClient.BillingResponseCode.NETWORK_ERROR -> PlayPurchaseUpdate.NETWORK_ERROR
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> PlayPurchaseUpdate.SERVICE_DISCONNECTED
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> PlayPurchaseUpdate.DEVELOPER_ERROR
        else -> PlayPurchaseUpdate.ERROR
    }

    private data class PurchaseOption(
        val productDetails: ProductDetails,
        val offerToken: String
    )

    private companion object {
        const val TAG = "InkwellBilling"
        const val BILLING_TIMEOUT_MS = 15_000L
    }
}
