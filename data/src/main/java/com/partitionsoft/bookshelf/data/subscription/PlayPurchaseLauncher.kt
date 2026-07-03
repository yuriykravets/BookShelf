package com.partitionsoft.bookshelf.data.subscription

import android.app.Activity
import kotlinx.coroutines.flow.Flow

enum class PlayPurchaseUpdate {
    LAUNCHED,
    PURCHASED,
    CANCELED,
    PENDING,
    ALREADY_OWNED,
    ITEM_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    NETWORK_ERROR,
    SERVICE_DISCONNECTED,
    DEVELOPER_ERROR,
    ACKNOWLEDGEMENT_FAILED,
    ERROR
}

interface PlayPurchaseLauncher {
    fun launchPurchase(activity: Activity, planId: String): PlayPurchaseUpdate
    fun observePurchaseUpdates(): Flow<PlayPurchaseUpdate>
}
