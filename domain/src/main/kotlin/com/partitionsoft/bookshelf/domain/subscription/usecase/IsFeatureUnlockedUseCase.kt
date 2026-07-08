package com.partitionsoft.bookshelf.domain.subscription.usecase

import com.partitionsoft.bookshelf.domain.subscription.model.PremiumFeature
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository

class IsFeatureUnlockedUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(feature: PremiumFeature): Boolean {
        val hasPremium = repository.getStatus() == SubscriptionStatus.PREMIUM
        return when (feature) {
            PremiumFeature.ADVANCED_READER_CUSTOMIZATION,
            PremiumFeature.UNLIMITED_HIGHLIGHTS,
            PremiumFeature.COLORED_HIGHLIGHTS,
            PremiumFeature.UNLIMITED_NOTES,
            PremiumFeature.UNLIMITED_INSIGHTS,
            PremiumFeature.KNOWLEDGE_EXPORT,
            PremiumFeature.CLOUD_SYNC,
            PremiumFeature.ADVANCED_READING_STATISTICS,
            PremiumFeature.BACKUP_AND_RESTORE,
            PremiumFeature.AI_READING_ASSISTANT -> hasPremium
        }
    }
}
