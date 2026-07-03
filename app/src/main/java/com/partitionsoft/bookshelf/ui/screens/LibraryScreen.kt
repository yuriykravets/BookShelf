package com.partitionsoft.bookshelf.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import com.partitionsoft.bookshelf.ui.LibraryEvent
import com.partitionsoft.bookshelf.ui.LibraryFilter
import com.partitionsoft.bookshelf.ui.LibraryViewModel
import com.partitionsoft.bookshelf.ui.ReadingBadge
import com.partitionsoft.bookshelf.ui.components.InkwellEmptyStateCard
import com.partitionsoft.bookshelf.ui.components.InkwellSectionTitle
import com.partitionsoft.bookshelf.ui.components.InkwellTopBar
import com.partitionsoft.bookshelf.ui.theme.LocalSpacing
import kotlinx.coroutines.delay

private val supportedReaderMimeTypes = arrayOf(
    "application/pdf",
    "application/epub+zip",
    "application/x-fictionbook+xml",
    "application/fb2+xml",
    "application/xml",
    "text/xml",
    "*/*"
)

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LibraryRoute(
    onBackClicked: (() -> Unit)?,
    onOpenDocument: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    var documentPendingDelete by remember { mutableStateOf<ReaderDocument?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importDocument(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.OpenDocument -> onOpenDocument(event.id)
                is LibraryEvent.Message -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageRes)
                )
            }
        }
    }

    documentPendingDelete?.let { document ->
        AlertDialog(
            onDismissRequest = { documentPendingDelete = null },
            title = { Text(text = stringResource(id = R.string.library_delete_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.library_delete_message,
                        document.title
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        documentPendingDelete = null
                        viewModel.deleteDocument(document.id)
                    }
                ) {
                    Text(text = stringResource(id = R.string.delete_book))
                }
            },
            dismissButton = {
                TextButton(onClick = { documentPendingDelete = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            InkwellTopBar(
                title = stringResource(id = R.string.library_title),
                onBackClick = onBackClicked,
                backContentDescription = stringResource(id = R.string.back),
                actions = {
                    IconButton(onClick = { importLauncher.launch(supportedReaderMimeTypes) }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.import_book)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            uiState.documents.isEmpty() -> {
                EmptyLibraryState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onImportClicked = { importLauncher.launch(supportedReaderMimeTypes) }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    item {
                        LibraryFiltersRow(
                            filters = uiState.availableFilters,
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = viewModel::selectFilter
                        )
                    }
                    if (uiState.badges.isNotEmpty()) {
                        item {
                            LibraryBadgesRow(badges = uiState.badges)
                        }
                    }
                    if (uiState.filteredDocuments.isEmpty()) {
                        item {
                            FilteredEmptyState()
                        }
                    } else {
                        items(uiState.filteredDocuments, key = { it.id }) { document ->
                            LibraryDocumentRow(
                                document = document,
                                onOpen = { onOpenDocument(document.id) },
                                onDelete = { documentPendingDelete = document }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryDocumentRow(
    document: ReaderDocument,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val spacing = LocalSpacing.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardShape = RoundedCornerShape(16.dp)
    val containerColor =
        MaterialTheme.colorScheme.surfaceColorAtElevation(if (isDarkTheme) 8.dp else 2.dp)
    val borderColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
    }

    Card(
        modifier = Modifier
            .shadow(
                elevation = if (isDarkTheme) 14.dp else 6.dp,
                shape = cardShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.22f else 0.10f),
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = if (isDarkTheme) 0.38f else 0.12f)
            )
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = cardShape,
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 8.dp else 3.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                    ) {
                        Text(
                            text = document.format.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!document.lastLocation.isNullOrBlank()) {
                        Text(
                            text = stringResource(
                                id = R.string.continue_reading_at,
                                document.lastLocation.toReadableLocation()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.delete_book)
                )
            }
        }
    }
}

private fun String?.toReadableLocation(): String =
    this?.substringBefore(':')?.toIntOrNull()?.plus(1)?.toString() ?: orEmpty()

@Composable
private fun EmptyLibraryState(
    modifier: Modifier = Modifier,
    onImportClicked: () -> Unit
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.Center
    ) {
        InkwellEmptyStateCard(
            title = stringResource(id = R.string.library_empty_title),
            message = stringResource(id = R.string.library_empty_subtitle),
            actionLabel = stringResource(id = R.string.import_book),
            onActionClick = onImportClicked
        )
    }
}

@Composable
private fun LibraryBadgesRow(badges: List<ReadingBadge>) {
    val spacing = LocalSpacing.current
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            InkwellSectionTitle(title = stringResource(id = R.string.library_badges_title))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                contentPadding = PaddingValues(vertical = spacing.xs)
            ) {
                itemsIndexed(badges, key = { _, badge -> badge.titleRes }) { index, badge ->
                    var visible by remember(badge.titleRes) { mutableStateOf(false) }
                    LaunchedEffect(badge.titleRes) {
                        delay(index * 90L)
                        visible = true
                    }
                    val targetScale = if (visible) 1f else 0.94f
                    val scale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = tween(durationMillis = 280),
                        label = "badge_scale"
                    )
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 280),
                                initialOffsetY = { it / 3 }
                            )
                    ) {
                        MilestoneBadgeCard(
                            badge = badge,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneBadgeCard(
    badge: ReadingBadge,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badge.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = badge.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(id = badge.supportingRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LibraryFiltersRow(
    filters: List<LibraryFilter>,
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit
) {
    val spacing = LocalSpacing.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        contentPadding = PaddingValues(vertical = spacing.xs)
    ) {
        items(filters, key = { filter -> filter::class.simpleName.orEmpty() }) { filter ->
            val label = when (filter) {
                LibraryFilter.All -> stringResource(id = R.string.library_filter_all)
                LibraryFilter.InProgress -> stringResource(id = R.string.library_filter_in_progress)
                LibraryFilter.Unread -> stringResource(id = R.string.library_filter_unread)
            }
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun FilteredEmptyState() {
    val spacing = LocalSpacing.current
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = stringResource(id = R.string.library_filter_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.library_filter_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
