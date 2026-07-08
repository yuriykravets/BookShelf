package com.partitionsoft.bookshelf.ui.premium

import androidx.compose.runtime.Composable
import com.partitionsoft.bookshelf.domain.subscription.model.PremiumFeature

@Composable
fun PremiumFeatureGate(
    feature: PremiumFeature,
    isUnlocked: Boolean,
    onUpgradeRequired: (PremiumFeature) -> Unit,
    onUnlocked: () -> Unit,
    content: @Composable (onClick: () -> Unit) -> Unit
) {
    content {
        if (isUnlocked) {
            onUnlocked()
        } else {
            onUpgradeRequired(feature)
        }
    }
}
