package com.partitionsoft.bookshelf.domain.subscription.model

enum class SubscriptionPeriod { MONTHLY, YEARLY }

data class SubscriptionPlan(
    val id: String,
    val period: SubscriptionPeriod,
    val formattedPrice: String,
    val isRecommended: Boolean = false
)

enum class SubscriptionActionResult {
    SUCCESS,
    PLAN_NOT_FOUND,
    NO_PURCHASE_FOUND
}
