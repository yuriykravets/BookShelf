package com.partitionsoft.bookshelf.ui.premium

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.domain.subscription.model.PremiumFeature

@Composable
fun PremiumUpgradeDialog(
    feature: PremiumFeature,
    onDismiss: () -> Unit,
    onViewPremium: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = stringResource(
                    id = R.string.premium_dialog_title,
                    feature.localizedTitle()
                )
            )
        },
        text = { Text(text = stringResource(id = R.string.premium_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onViewPremium) {
                Text(text = stringResource(id = R.string.premium_dialog_view_premium))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.not_now))
            }
        }
    )
}
