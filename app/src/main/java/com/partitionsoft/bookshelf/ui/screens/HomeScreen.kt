package com.partitionsoft.bookshelf.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.domain.model.Book
import com.partitionsoft.bookshelf.domain.model.BookCategory
import com.partitionsoft.bookshelf.domain.model.BookSection
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import com.partitionsoft.bookshelf.domain.model.ReaderDocumentFormat
import com.partitionsoft.bookshelf.domain.model.SectionLayout
import com.partitionsoft.bookshelf.ui.BooksUiState
import com.partitionsoft.bookshelf.ui.CategoryShelfUiState
import com.partitionsoft.bookshelf.ui.HomeUiState
import com.partitionsoft.bookshelf.ui.components.InkwellSectionTitle
import com.partitionsoft.bookshelf.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import retrofit2.HttpException

@Composable
fun HomeScreen(
    homeUiState: HomeUiState,
    categoryShelfUiState: CategoryShelfUiState,
    booksUiState: BooksUiState,
    isSearchActive: Boolean,
    retryAction: () -> Unit,
    onCategorySelected: (BookCategory) -> Unit,
    modifier: Modifier = Modifier,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit,
    onSearchRetry: () -> Unit,
    onBrowseRequested: (title: String, query: String, orderBy: String?, filter: String?) -> Unit,
    onAiAssistantClicked: () -> Unit,
    onContinueReadingClicked: (Long) -> Unit,
    onStreakClicked: () -> Unit
) {
    val hasHomeContent = homeUiState.hasRenderableContent()

    if (isSearchActive) {
        SearchResults(
            booksUiState = booksUiState,
            onRetry = onSearchRetry,
            modifier = modifier,
            onBookClicked = onBookClicked,
            onFavoriteClicked = onFavoriteClicked
        )
        return
    }

    when {
        homeUiState.isLoading -> LoadingScreen(modifier = modifier.fillMaxSize())
        homeUiState.error != null && !hasHomeContent -> {
            if (homeUiState.error.isQuotaLimitIssue()) {
                HomeQuotaErrorScreen(
                    retryAction = retryAction,
                    modifier = modifier.fillMaxSize()
                )
            } else {
                ErrorScreen(
                    retryAction = retryAction,
                    modifier = modifier.fillMaxSize()
                )
            }
        }

        else -> HomeFeedList(
            homeUiState = homeUiState,
            homeError = homeUiState.error,
            categoryShelfUiState = categoryShelfUiState,
            onCategorySelected = onCategorySelected,
            modifier = modifier,
            onBookClicked = onBookClicked,
            onFavoriteClicked = onFavoriteClicked,
            onBrowseRequested = onBrowseRequested,
            onAiAssistantClicked = onAiAssistantClicked,
            onContinueReadingClicked = onContinueReadingClicked,
            onStreakClicked = onStreakClicked,
            onRetryHome = retryAction
        )
    }
}

@Composable
private fun HomeQuotaErrorScreen(
    retryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.home_quota_status_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = R.string.home_quota_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(id = R.string.home_quota_supporting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = retryAction) {
            Text(text = stringResource(id = R.string.retry))
        }
    }
}

private fun Throwable.isQuotaLimitIssue(): Boolean {
    return generateSequence(this) { it.cause }.any { throwable ->
        when (throwable) {
            is HttpException -> throwable.code() == 429
            else -> {
                val message = throwable.message.orEmpty().lowercase()
                message.contains("quota") ||
                        message.contains("resource_exhausted") ||
                        message.contains("rate limit")
            }
        }
    }
}

@Composable
private fun SearchResults(
    booksUiState: BooksUiState,
    onRetry: () -> Unit,
    modifier: Modifier,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit
) {
    when (booksUiState) {
        BooksUiState.Loading -> LoadingScreen(modifier = modifier.fillMaxSize())
        is BooksUiState.Success -> {
            if (booksUiState.bookSearch.isEmpty()) {
                EmptyScreen(onRefresh = onRetry)
            } else {
                BooksGridScreen(
                    books = booksUiState.bookSearch,
                    modifier = Modifier.fillMaxSize(),
                    onBookClicked = onBookClicked,
                    onFavoriteClicked = onFavoriteClicked
                )
            }
        }

        BooksUiState.Error -> ErrorScreen(
            retryAction = onRetry,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun HomeFeedList(
    homeUiState: HomeUiState,
    homeError: Throwable?,
    categoryShelfUiState: CategoryShelfUiState,
    onCategorySelected: (BookCategory) -> Unit,
    modifier: Modifier,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit,
    onBrowseRequested: (title: String, query: String, orderBy: String?, filter: String?) -> Unit,
    onAiAssistantClicked: () -> Unit,
    onContinueReadingClicked: (Long) -> Unit,
    onStreakClicked: () -> Unit,
    onRetryHome: () -> Unit
) {
    var streakBannerDismissed by rememberSaveable(
        homeUiState.streakDays,
        homeUiState.todayMinutes
    ) { mutableStateOf(false) }

    val selectedCategory =
        remember(homeUiState.categories, categoryShelfUiState.selectedCategoryId) {
            homeUiState.categories.firstOrNull { it.id == categoryShelfUiState.selectedCategoryId }
        }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
    ) {
        item(key = "home_intro") {
            HomeIntroCard(onAiAssistantClicked = onAiAssistantClicked)
        }

        homeError?.let { error ->
            item(key = "home_status_banner") {
                HomeInlineStatusCard(
                    isQuotaIssue = error.isQuotaLimitIssue(),
                    onRetry = onRetryHome
                )
            }
        }

        if (homeUiState.streakDays > 0) {
            item(key = "home_streak_banner") {
                val streakBannerVisibleState = remember(homeUiState.streakDays, homeUiState.todayMinutes) {
                    MutableTransitionState(false).apply {
                        targetState = true
                    }
                }
                LaunchedEffect(streakBannerDismissed) {
                    streakBannerVisibleState.targetState = !streakBannerDismissed
                }

                AnimatedVisibility(
                    visibleState = streakBannerVisibleState,
                    enter = fadeIn(animationSpec = tween(durationMillis = 240)) +
                            slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight / 3 },
                                animationSpec = tween(durationMillis = 240)
                            ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight / 3 },
                                animationSpec = tween(durationMillis = 180)
                            )
                ) {
                    HomeStreakBanner(
                        streakDays = homeUiState.streakDays,
                        todayMinutes = homeUiState.todayMinutes,
                        onOpenStats = onStreakClicked,
                        onDismiss = { streakBannerDismissed = true }
                    )
                }
            }
        }

        homeUiState.continueReading?.let { document ->
            item(key = "continue_reading") {
                SectionHeader(title = stringResource(id = R.string.home_continue_reading_title))
                ContinueReadingCard(
                    document = document,
                    onOpen = { onContinueReadingClicked(document.id) }
                )
            }
        }

        if (homeUiState.categories.isNotEmpty()) {
            item(key = "categories_block") {
                SectionHeader(title = stringResource(id = R.string.home_categories_title))
                CategoryRow(
                    categories = homeUiState.categories,
                    selectedId = categoryShelfUiState.selectedCategoryId,
                    onCategorySelected = onCategorySelected
                )
                if (
                    categoryShelfUiState.isLoading ||
                    categoryShelfUiState.books.isNotEmpty() ||
                    categoryShelfUiState.error != null
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryShelfSection(
                        selectedCategory = selectedCategory,
                        categoryShelfUiState = categoryShelfUiState,
                        onRetry = selectedCategory?.let { { onCategorySelected(it) } },
                        onBookClicked = onBookClicked,
                        onFavoriteClicked = onFavoriteClicked,
                        onBrowseRequested = onBrowseRequested
                    )
                }
            }
        }
        items(items = homeUiState.sections, key = { it.id }) { section ->
            HomeSection(
                section = section,
                onBookClicked = onBookClicked,
                onFavoriteClicked = onFavoriteClicked,
                onBrowseRequested = onBrowseRequested
            )
        }
    }
}

@Composable
private fun HomeStreakBanner(
    streakDays: Int,
    todayMinutes: Int,
    onOpenStats: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenStats),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.home_streak_banner_title, streakDays),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(id = R.string.home_streak_banner_subtitle, todayMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.clear_action),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun HomeIntroCard(onAiAssistantClicked: () -> Unit) {
    val cardBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_intro_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.home_intro_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAiAssistantClicked) {
                Text(text = stringResource(id = R.string.home_ai_assistant_action))
            }
        }
    }
}

@Composable
private fun HomeInlineStatusCard(
    isQuotaIssue: Boolean,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isQuotaIssue) {
                    stringResource(id = R.string.home_quota_status_label)
                } else {
                    stringResource(id = R.string.loading_failed)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isQuotaIssue) {
                    stringResource(id = R.string.home_quota_supporting)
                } else {
                    stringResource(id = R.string.error_supporting_copy)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(id = R.string.retry))
            }
        }
    }
}

private fun HomeUiState.hasRenderableContent(): Boolean =
    featured.isNotEmpty() || sections.isNotEmpty() || categories.isNotEmpty() || continueReading != null

@Composable
private fun ContinueReadingCard(
    document: ReaderDocument,
    onOpen: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = continueReadingProgressLabel(document),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onOpen) {
                Text(text = stringResource(id = R.string.home_continue_reading_action))
            }
        }
    }
}

@Composable
private fun continueReadingProgressLabel(document: ReaderDocument): String {
    val index = document.lastLocation?.substringBefore(':')?.toIntOrNull()?.plus(1)
    return when (document.format) {
        ReaderDocumentFormat.PDF -> {
            if (index != null && index > 0) {
                stringResource(id = R.string.home_continue_page, index)
            } else {
                document.format.name
            }
        }

        ReaderDocumentFormat.EPUB,
        ReaderDocumentFormat.FB2 -> {
            if (index != null && index > 0) {
                stringResource(id = R.string.home_continue_chapter, index)
            } else {
                document.format.name
            }
        }

        ReaderDocumentFormat.UNKNOWN -> document.format.name
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    InkwellSectionTitle(
        title = title,
        actionLabel = actionLabel,
        onActionClick = onActionClick,
        modifier = Modifier.padding(bottom = spacing.sm)
    )
}

@Composable
private fun FeaturedSection(
    books: List<Book>,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(books, key = { it.id }) { book ->
            FeaturedBookCard(
                book = book,
                onBookClicked = onBookClicked,
                onFavoriteClicked = onFavoriteClicked
            )
        }
    }
}

@Composable
private fun FeaturedBookCard(
    book: Book,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit
) {
    val cardBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable { onBookClicked(book) },
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FavoriteToggleButton(
                    isFavorite = book.isFavorite,
                    onClick = { onFavoriteClicked(book) },
                    style = FavoriteToggleStyle.Compact,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
            }
            BookCover(
                thumbnail = book.thumbnail,
                title = book.title,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                useDefaultAspectRatio = false
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            BookRating(
                rating = book.rating,
                ratingsCount = book.ratingsCount,
                compact = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (book.authors.isNotEmpty()) {
                Text(
                    text = book.authors.joinToString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<BookCategory>,
    selectedId: String?,
    onCategorySelected: (BookCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = category.id == selectedId
            CategoryChip(
                text = category.title,
                selected = isSelected,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CategoryShelfSection(
    selectedCategory: BookCategory?,
    categoryShelfUiState: CategoryShelfUiState,
    onRetry: (() -> Unit)?,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit,
    onBrowseRequested: (title: String, query: String, orderBy: String?, filter: String?) -> Unit
) {
    var lastAutoRetriedCategoryId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(categoryShelfUiState.error, categoryShelfUiState.selectedCategoryId, onRetry) {
        val selectedId = categoryShelfUiState.selectedCategoryId
        if (categoryShelfUiState.error != null && onRetry != null && selectedId != lastAutoRetriedCategoryId) {
            lastAutoRetriedCategoryId = selectedId
            delay(900)
            onRetry()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val title = selectedCategory?.title ?: stringResource(id = R.string.home_categories_title)
        SectionHeader(
            title = stringResource(id = R.string.home_category_shelf_title, title),
            actionLabel = stringResource(id = R.string.see_all),
            onActionClick = selectedCategory?.let {
                { onBrowseRequested(it.title, it.query, POPULAR_ORDER, null) }
            }
        )
        AnimatedContent(
            targetState = categoryShelfUiState,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(200)
                ) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "category_shelf"
        ) { state ->
            when {
                state.isLoading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(4) {
                                ShimmerBookCard(modifier = Modifier.width(150.dp))
                            }
                        }
                    }
                }

                state.error != null -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.home_category_error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(4) {
                                ShimmerBookCard(modifier = Modifier.width(150.dp))
                            }
                        }
                    }
                }

                state.books.isEmpty() -> {
                    Text(
                        text = stringResource(id = R.string.empty_results_supporting),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                else -> {
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.books, key = { it.id }) { book ->
                            HorizontalBookCard(
                                book = book,
                                onBookClicked = onBookClicked,
                                onFavoriteClicked = onFavoriteClicked
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSection(
    section: BookSection,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit,
    onBrowseRequested: (title: String, query: String, orderBy: String?, filter: String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionHeader(
            title = section.title,
            actionLabel = stringResource(id = R.string.see_all),
            onActionClick = {
                onBrowseRequested(
                    section.title,
                    sectionQuery(section),
                    sectionOrderBy(section),
                    sectionFilter(section)
                )
            }
        )
        when (section.layout) {
            SectionLayout.Carousel -> FeaturedSection(
                books = section.books,
                onBookClicked = onBookClicked,
                onFavoriteClicked = onFavoriteClicked
            )

            SectionLayout.Horizontal -> LazyRow(
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(section.books, key = { it.id }) { book ->
                    HorizontalBookCard(
                        book = book,
                        onBookClicked = onBookClicked,
                        onFavoriteClicked = onFavoriteClicked
                    )
                }
            }
        }
    }
}

private fun sectionQuery(section: BookSection): String = when (section.id) {
    "old_classics" -> "subject:classic literature"
    "design_spotlight" -> "design thinking"
    "business_moves" -> "subject:business"
    "science_breakthroughs" -> "subject:science"
    else -> section.title
}

private fun sectionOrderBy(section: BookSection): String? = when (section.id) {
    else -> POPULAR_ORDER
}

private fun sectionFilter(section: BookSection): String? = when (section.id) {
    else -> null
}

private const val POPULAR_ORDER = "popular"

@Composable
private fun HorizontalBookCard(
    book: Book,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit
) {
    val cardBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onBookClicked(book) },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FavoriteToggleButton(
                    isFavorite = book.isFavorite,
                    onClick = { onFavoriteClicked(book) },
                    style = FavoriteToggleStyle.Compact,
                    buttonSize = 34.dp,
                    iconSize = 20.dp
                )
            }
            BookCover(
                thumbnail = book.thumbnail,
                title = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                useDefaultAspectRatio = false
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            BookRating(
                rating = book.rating,
                ratingsCount = book.ratingsCount,
                compact = true
            )
        }
    }
}

@Composable
private fun EmptyScreen(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.empty_results_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.empty_results_supporting),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRefresh) {
            Text(text = stringResource(id = R.string.try_another_search))
        }
    }
}
