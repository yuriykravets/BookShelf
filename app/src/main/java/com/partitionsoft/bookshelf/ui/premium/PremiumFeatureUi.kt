package com.partitionsoft.bookshelf.ui.premium

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.domain.subscription.model.PremiumFeature

internal val PremiumFeature.icon: ImageVector
    get() = when (this) {
        PremiumFeature.ADVANCED_READER_CUSTOMIZATION -> Icons.Filled.TextFields
        PremiumFeature.UNLIMITED_HIGHLIGHTS -> Icons.Filled.FormatPaint
        PremiumFeature.COLORED_HIGHLIGHTS -> Icons.Filled.ColorLens
        PremiumFeature.UNLIMITED_NOTES -> Icons.Filled.EditNote
        PremiumFeature.UNLIMITED_INSIGHTS -> Icons.Filled.Lightbulb
        PremiumFeature.KNOWLEDGE_EXPORT -> Icons.Filled.Share
        PremiumFeature.CLOUD_SYNC -> Icons.Filled.CloudSync
        PremiumFeature.ADVANCED_READING_STATISTICS -> Icons.Filled.Insights
        PremiumFeature.BACKUP_AND_RESTORE -> Icons.Filled.Backup
        PremiumFeature.AI_READING_ASSISTANT -> Icons.Filled.AutoAwesome
    }

@Composable
internal fun PremiumFeature.localizedTitle(): String = stringResource(
    id = when (this) {
        PremiumFeature.ADVANCED_READER_CUSTOMIZATION -> R.string.premium_feature_reader_title
        PremiumFeature.UNLIMITED_HIGHLIGHTS -> R.string.premium_feature_highlights_title
        PremiumFeature.COLORED_HIGHLIGHTS -> R.string.premium_feature_colors_title
        PremiumFeature.UNLIMITED_NOTES -> R.string.premium_feature_notes_title
        PremiumFeature.UNLIMITED_INSIGHTS -> R.string.premium_feature_insights_title
        PremiumFeature.KNOWLEDGE_EXPORT -> R.string.premium_feature_export_title
        PremiumFeature.CLOUD_SYNC -> R.string.premium_feature_sync_title
        PremiumFeature.ADVANCED_READING_STATISTICS -> R.string.premium_feature_stats_title
        PremiumFeature.BACKUP_AND_RESTORE -> R.string.premium_feature_backup_title
        PremiumFeature.AI_READING_ASSISTANT -> R.string.premium_feature_ai_title
    }
)

@Composable
internal fun PremiumFeature.localizedDescription(): String = stringResource(
    id = when (this) {
        PremiumFeature.ADVANCED_READER_CUSTOMIZATION -> R.string.premium_feature_reader_description
        PremiumFeature.UNLIMITED_HIGHLIGHTS -> R.string.premium_feature_highlights_description
        PremiumFeature.COLORED_HIGHLIGHTS -> R.string.premium_feature_colors_description
        PremiumFeature.UNLIMITED_NOTES -> R.string.premium_feature_notes_description
        PremiumFeature.UNLIMITED_INSIGHTS -> R.string.premium_feature_insights_description
        PremiumFeature.KNOWLEDGE_EXPORT -> R.string.premium_feature_export_description
        PremiumFeature.CLOUD_SYNC -> R.string.premium_feature_sync_description
        PremiumFeature.ADVANCED_READING_STATISTICS -> R.string.premium_feature_stats_description
        PremiumFeature.BACKUP_AND_RESTORE -> R.string.premium_feature_backup_description
        PremiumFeature.AI_READING_ASSISTANT -> R.string.premium_feature_ai_description
    }
)
