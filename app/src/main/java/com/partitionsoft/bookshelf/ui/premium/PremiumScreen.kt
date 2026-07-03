package com.partitionsoft.bookshelf.ui.premium

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookshelf.R
import com.example.bookshelf.BuildConfig
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPeriod
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionBillingAvailability
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionPlan
import com.partitionsoft.bookshelf.ui.components.InkwellPrimaryButton
import com.partitionsoft.bookshelf.ui.components.InkwellSupportText
import com.partitionsoft.bookshelf.ui.components.InkwellTopBar
import com.partitionsoft.bookshelf.ui.theme.LocalSpacing

@Composable
fun PremiumRoute(
    onBackClicked: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveSelectedPlanId = selectedPlanId
        ?: uiState.plans.firstOrNull { it.isRecommended }?.id
        ?: uiState.plans.firstOrNull()?.id
    val actionMessage = uiState.message?.let { message ->
        stringResource(
            when (message) {
                PremiumActionMessage.PURCHASED -> R.string.premium_local_purchased
                PremiumActionMessage.PURCHASE_CANCELLED -> R.string.premium_purchase_cancelled
                PremiumActionMessage.PURCHASE_PENDING -> R.string.premium_purchase_pending
                PremiumActionMessage.ALREADY_OWNED -> R.string.premium_purchase_already_owned
                PremiumActionMessage.ITEM_UNAVAILABLE -> R.string.premium_purchase_item_unavailable
                PremiumActionMessage.BILLING_UNAVAILABLE -> R.string.premium_purchase_billing_unavailable
                PremiumActionMessage.NETWORK_ERROR -> R.string.premium_purchase_network_error
                PremiumActionMessage.ACKNOWLEDGEMENT_FAILED -> R.string.premium_purchase_acknowledgement_failed
                PremiumActionMessage.RESTORED -> R.string.premium_local_restored
                PremiumActionMessage.NOTHING_TO_RESTORE -> R.string.premium_local_nothing_to_restore
                PremiumActionMessage.CANCELLED -> R.string.premium_local_cancelled
                PremiumActionMessage.ERROR -> R.string.premium_local_error
            }
        )
    }

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            snackbarHostState.showSnackbar(actionMessage)
            viewModel.consumeMessage()
        }
    }

    PremiumScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClicked = onBackClicked,
        isLocalBillingAvailable = BuildConfig.DEBUG,
        selectedPlanId = effectiveSelectedPlanId,
        onPlanSelected = { selectedPlanId = it },
        onPurchase = { planId ->
            activity?.let { viewModel.purchase(planId, it) }
        },
        onCancel = viewModel::cancel,
        onManageSubscription = {
            val uri = Uri.parse(
                "https://play.google.com/store/account/subscriptions?sku=${BuildConfig.PREMIUM_PRODUCT_ID}&package=${context.packageName}"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        },
        onRetryBilling = viewModel::retryBilling
    )
}

@Composable
fun PremiumScreen(
    uiState: PremiumUiState,
    snackbarHostState: SnackbarHostState,
    onBackClicked: () -> Unit,
    isLocalBillingAvailable: Boolean,
    selectedPlanId: String?,
    onPlanSelected: (String) -> Unit,
    onPurchase: (String) -> Unit,
    onCancel: () -> Unit,
    onManageSubscription: () -> Unit,
    onRetryBilling: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            InkwellTopBar(
                title = stringResource(id = R.string.premium_title),
                onBackClick = onBackClicked,
                backContentDescription = stringResource(id = R.string.back)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            else -> PremiumContent(
                uiState = uiState,
                isLocalBillingAvailable = isLocalBillingAvailable,
                selectedPlanId = selectedPlanId,
                onPlanSelected = onPlanSelected,
                onPurchase = onPurchase,
                onCancel = onCancel,
                onManageSubscription = onManageSubscription,
                onRetryBilling = onRetryBilling,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun PremiumContent(
    uiState: PremiumUiState,
    isLocalBillingAvailable: Boolean,
    selectedPlanId: String?,
    onPlanSelected: (String) -> Unit,
    onPurchase: (String) -> Unit,
    onCancel: () -> Unit,
    onManageSubscription: () -> Unit,
    onRetryBilling: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = spacing.lg,
            vertical = spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item {
            PremiumHero(isPremium = uiState.isPremium)
        }

        item {
            when {
                isLocalBillingAvailable || uiState.isPremium ||
                    uiState.billingAvailability == SubscriptionBillingAvailability.AVAILABLE -> {
                LocalBillingCard(
                    uiState = uiState,
                    isLocalBilling = isLocalBillingAvailable,
                    selectedPlanId = selectedPlanId,
                    onPlanSelected = onPlanSelected,
                    onPurchase = onPurchase,
                    onCancel = onCancel,
                    onManageSubscription = onManageSubscription
                )
                }
                uiState.billingAvailability == SubscriptionBillingAvailability.LOADING -> {
                    BillingLoadingCard()
                }
                else -> {
                    BillingUnavailableCard(onRetry = onRetryBilling)
                }
            }
        }

        if (uiState.hasError) {
            item {
                InkwellSupportText(
                    text = stringResource(id = R.string.premium_status_error),
                    centered = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Text(
                text = stringResource(id = R.string.premium_benefits_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(workingPremiumBenefits(), key = { it.titleRes }) { benefit ->
            PremiumBenefitCard(benefit = benefit)
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.premium_future_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    InkwellSupportText(text = stringResource(R.string.premium_future_description))
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                if (uiState.isPremium) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.premium_active),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                InkwellSupportText(
                    text = stringResource(id = R.string.premium_free_reading_promise),
                    centered = true
                )
            }
        }
    }
}

@Composable
private fun BillingLoadingCard() {
    val spacing = LocalSpacing.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            InkwellSupportText(text = stringResource(R.string.premium_billing_loading))
        }
    }
}

@Composable
private fun BillingUnavailableCard(onRetry: () -> Unit) {
    val spacing = LocalSpacing.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.premium_billing_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            InkwellSupportText(text = stringResource(R.string.premium_billing_unavailable))
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.premium_billing_retry))
            }
        }
    }
}

@Composable
private fun LocalBillingCard(
    uiState: PremiumUiState,
    isLocalBilling: Boolean,
    selectedPlanId: String?,
    onPlanSelected: (String) -> Unit,
    onPurchase: (String) -> Unit,
    onCancel: () -> Unit,
    onManageSubscription: () -> Unit
) {
    val spacing = LocalSpacing.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = stringResource(
                    if (isLocalBilling) R.string.premium_local_billing_title else R.string.premium_plans_title
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            InkwellSupportText(
                text = stringResource(
                    if (isLocalBilling) R.string.premium_local_billing_description else R.string.premium_plans_description
                )
            )

            if (uiState.isPremium) {
                val activePlan = uiState.plans.firstOrNull { it.id == uiState.activePlanId }
                    ?: uiState.plans.singleOrNull()
                Text(
                    text = activePlan?.let { plan ->
                        stringResource(
                            R.string.premium_local_active_plan,
                            plan.period.localizedLabel(),
                            plan.formattedPrice
                        )
                    } ?: stringResource(R.string.premium_subscription_active),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = if (isLocalBilling) onCancel else onManageSubscription,
                    enabled = !uiState.isProcessing
                ) {
                    Text(
                        stringResource(
                            if (isLocalBilling) R.string.premium_local_cancel else R.string.premium_manage_subscription
                        )
                    )
                }
            } else {
                uiState.plans.forEach { plan ->
                    PlanRow(
                        plan = plan,
                        selected = selectedPlanId == plan.id,
                        onClick = { onPlanSelected(plan.id) }
                    )
                }
                InkwellPrimaryButton(
                    text = stringResource(
                        if (isLocalBilling) R.string.premium_local_subscribe else R.string.premium_play_subscribe
                    ),
                    onClick = { selectedPlanId?.let(onPurchase) },
                    enabled = selectedPlanId != null && !uiState.isProcessing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PlanRow(plan: SubscriptionPlan, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(plan.period.localizedLabel(), style = MaterialTheme.typography.titleMedium)
            if (plan.isRecommended) {
                Text(
                    stringResource(R.string.premium_local_recommended),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(plan.formattedPrice, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SubscriptionPeriod.localizedLabel(): String = stringResource(
    if (this == SubscriptionPeriod.MONTHLY) R.string.premium_plan_monthly else R.string.premium_plan_yearly
)

@Composable
private fun PremiumHero(isPremium: Boolean) {
    val spacing = LocalSpacing.current
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(
                    id = if (isPremium) R.string.premium_hero_active_title else R.string.premium_hero_title
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.premium_hero_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PremiumBenefitCard(benefit: PremiumBenefitUi) {
    val spacing = LocalSpacing.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = benefit.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = stringResource(benefit.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(benefit.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun workingPremiumBenefits(): List<PremiumBenefitUi> = listOf(
    PremiumBenefitUi(
        icon = Icons.Filled.TextFields,
        titleRes = R.string.premium_working_typography_title,
        descriptionRes = R.string.premium_working_typography_description
    ),
    PremiumBenefitUi(
        icon = Icons.Filled.ColorLens,
        titleRes = R.string.premium_working_themes_title,
        descriptionRes = R.string.premium_working_themes_description
    ),
    PremiumBenefitUi(
        icon = Icons.Filled.FormatLineSpacing,
        titleRes = R.string.premium_working_layout_title,
        descriptionRes = R.string.premium_working_layout_description
    ),
    PremiumBenefitUi(
        icon = Icons.Filled.BorderColor,
        titleRes = R.string.premium_working_annotations_title,
        descriptionRes = R.string.premium_working_annotations_description
    )
)

private data class PremiumBenefitUi(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int
)
