package com.partitionsoft.bookshelf.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.MaterialTheme as Material2Theme
import androidx.compose.material.Surface as Material2Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.domain.model.Book
import com.partitionsoft.bookshelf.domain.model.BookCategory
import com.partitionsoft.bookshelf.ui.navigation.BooksDestinations
import com.partitionsoft.bookshelf.ui.premium.PremiumRoute
import com.partitionsoft.bookshelf.ui.screens.BookDetailsRoute
import com.partitionsoft.bookshelf.ui.screens.BrowseBooksRoute
import com.partitionsoft.bookshelf.ui.screens.FavoritesRoute
import com.partitionsoft.bookshelf.ui.screens.AiAssistantRoute
import com.partitionsoft.bookshelf.ui.screens.HomeScreen
import com.partitionsoft.bookshelf.ui.screens.LibraryRoute
import com.partitionsoft.bookshelf.ui.screens.LocalReaderRoute
import com.partitionsoft.bookshelf.ui.screens.MainAppBar
import com.partitionsoft.bookshelf.ui.screens.ReaderRoute
import com.partitionsoft.bookshelf.ui.screens.StatsRoute
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun BooksApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val topLevelDestinations = remember {
        listOf(
            TopLevelDestination(
                route = BooksDestinations.HOME_ROUTE,
                icon = Icons.Filled.Home,
                labelRes = R.string.bottom_nav_home
            ),
            TopLevelDestination(
                route = BooksDestinations.LIBRARY_ROUTE,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                labelRes = R.string.bottom_nav_read
            ),
            TopLevelDestination(
                route = BooksDestinations.STATS_ROUTE,
                icon = Icons.Filled.Insights,
                labelRes = R.string.bottom_nav_stats
            )
        )
    }
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(BooksDestinations.HOME_ROUTE)
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(id = destination.labelRes)
                                )
                            },
                            label = { Text(text = stringResource(id = destination.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BooksDestinations.HOME_ROUTE,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {

        composable(route = BooksDestinations.HOME_ROUTE) {
            HomeRoute(
                onBookClicked = {
                    navController.navigate(BooksDestinations.detailsRoute(it.id))
                },
                onBrowseRequested = { title, query, orderBy, filter ->
                    navController.navigate(
                        BooksDestinations.browseRoute(
                            title = title,
                            query = query,
                            orderBy = orderBy,
                            filter = filter
                        )
                    )
                },
                onFavoritesClicked = {
                    navController.navigate(BooksDestinations.FAVORITES_ROUTE)
                },
                onAiAssistantClicked = {
                    navController.navigate(BooksDestinations.AI_ASSISTANT_ROUTE)
                },
                onContinueReadingClicked = { documentId ->
                    navController.navigate(BooksDestinations.localReaderRoute(documentId))
                },
                onStreakClicked = {
                    navController.navigate(BooksDestinations.STATS_ROUTE)
                },
                onPremiumClicked = {
                    navController.navigate(BooksDestinations.PREMIUM_ROUTE)
                }
            )
        }

        composable(route = BooksDestinations.PREMIUM_ROUTE) {
            PremiumRoute(onBackClicked = navController::navigateUp)
        }

        composable(route = BooksDestinations.AI_ASSISTANT_ROUTE) {
            AiAssistantRoute(
                onBackClicked = navController::navigateUp,
                onBookClicked = { book ->
                    navController.navigate(BooksDestinations.detailsRoute(book.id))
                }
            )
        }

        composable(route = BooksDestinations.LIBRARY_ROUTE) {
            LibraryRoute(
                onBackClicked = navController::navigateUp,
                onOpenDocument = { documentId ->
                    navController.navigate(BooksDestinations.localReaderRoute(documentId))
                }
            )
        }

        composable(route = BooksDestinations.FAVORITES_ROUTE) {
            FavoritesRoute(
                onBackClicked = navController::navigateUp,
                onBookClicked = { book ->
                    navController.navigate(BooksDestinations.detailsRoute(book.id))
                }
            )
        }

        composable(route = BooksDestinations.STATS_ROUTE) {
            StatsRoute(onBackClicked = navController::navigateUp)
        }

        composable(
            route = BooksDestinations.BROWSE_ROUTE,
            arguments = listOf(
                navArgument(BooksDestinations.TITLE_ARG) { type = NavType.StringType },
                navArgument(BooksDestinations.QUERY_ARG) { type = NavType.StringType },
                navArgument(BooksDestinations.ORDER_BY_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(BooksDestinations.FILTER_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            BrowseBooksRoute(
                onBackClicked = navController::navigateUp,
                onBookClicked = { book ->
                    navController.navigate(BooksDestinations.detailsRoute(book.id))
                }
            )
        }

        composable(
            route = BooksDestinations.DETAILS_ROUTE,
            arguments = listOf(navArgument(BooksDestinations.BOOK_ID_ARG) { type = NavType.StringType })
        ) {
            BookDetailsRoute(
                onBackClicked = navController::navigateUp,
                onReadClicked = { bookId ->
                    navController.navigate(BooksDestinations.readerRoute(bookId))
                },
                onPreviewClicked = { previewLink ->
                    navController.context.startActivity(Intent(Intent.ACTION_VIEW, previewLink.toUri()))
                },
                onShareClicked = { shareText ->
                    val appInvite = navController.context.getString(
                        R.string.share_install_cta,
                        navController.context.getString(R.string.app_name)
                    )
                    val combinedShareText = buildString {
                        append(shareText)
                        append("\n\n")
                        append(appInvite)
                        append("\n")
                        append(navController.context.getString(R.string.share_app_link))
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, combinedShareText)
                    }
                    navController.context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            navController.context.getString(R.string.share_book_chooser)
                        )
                    )
                }
            )
        }

        composable(
            route = BooksDestinations.READER_ROUTE,
            arguments = listOf(navArgument(BooksDestinations.BOOK_ID_ARG) { type = NavType.StringType })
        ) {
            ReaderRoute(onBackClicked = navController::navigateUp)
        }

        composable(
            route = BooksDestinations.LOCAL_READER_ROUTE,
            arguments = listOf(navArgument(BooksDestinations.DOCUMENT_ID_ARG) { type = NavType.LongType })
        ) {
            LocalReaderRoute(
                onBackClicked = navController::navigateUp,
                onPremiumRequested = {
                    navController.navigate(BooksDestinations.PREMIUM_ROUTE)
                }
            )
        }
        }
    }
}

@Composable
private fun HomeRoute(
    onBookClicked: (Book) -> Unit,
    onBrowseRequested: (title: String, query: String, orderBy: String?, filter: String?) -> Unit,
    onFavoritesClicked: () -> Unit,
    onAiAssistantClicked: () -> Unit,
    onContinueReadingClicked: (Long) -> Unit,
    onStreakClicked: () -> Unit,
    onPremiumClicked: () -> Unit
) {
    val booksViewModel: BooksViewModel = hiltViewModel()

    val searchUiState by booksViewModel.searchUiState.collectAsStateWithLifecycle()
    val homeUiState by booksViewModel.homeUiState.collectAsStateWithLifecycle()
    val categoryUiState by booksViewModel.categoryUiState.collectAsStateWithLifecycle()
    val searchWidgetState by booksViewModel.searchWidgetState.collectAsStateWithLifecycle()
    val searchTextState by booksViewModel.searchTextState.collectAsStateWithLifecycle()
    val isSearchActive =
        searchWidgetState == BooksViewModel.SearchWidgetState.OPENED || searchTextState.isNotBlank()
    var shouldPlayAiFabIntro by remember { mutableStateOf(true) }

    BackHandler(enabled = isSearchActive) {
        booksViewModel.closeSearch()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(id = R.string.loading_failed)
    val retryLabel = stringResource(id = R.string.retry)

    LaunchedEffect(searchUiState, errorMessage, retryLabel) {
        if (searchUiState is BooksUiState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                booksViewModel.getBooks()
            }
        }
    }

    BooksAppContent(
        snackbarHostState = snackbarHostState,
        searchWidgetState = searchWidgetState,
        searchTextState = searchTextState,
        onTextChange = booksViewModel::updateSearchTextState,
        onCloseClicked = booksViewModel::closeSearch,
        onSearchClicked = booksViewModel::getBooks,
        onSearchTriggered = {
            booksViewModel.updateSearchWidgetState(newValue = BooksViewModel.SearchWidgetState.OPENED)
        },
        onFavoritesClicked = onFavoritesClicked,
        onAiAssistantClicked = onAiAssistantClicked,
        booksUiState = searchUiState,
        homeUiState = homeUiState,
        categoryUiState = categoryUiState,
        isSearchActive = isSearchActive,
        onCategorySelected = booksViewModel::loadCategory,
        onHomeRetry = booksViewModel::refreshHome,
        searchRetryAction = { booksViewModel.getBooks(searchTextState) },
        onBookClicked = onBookClicked,
        onFavoriteClicked = booksViewModel::onFavoriteClicked,
        onBrowseRequested = onBrowseRequested,
        onContinueReadingClicked = onContinueReadingClicked,
        onStreakClicked = onStreakClicked,
        onPremiumClicked = onPremiumClicked,
        shouldPlayAiFabIntro = shouldPlayAiFabIntro,
        onAiFabIntroFinished = { shouldPlayAiFabIntro = false }
    )
}

@Composable
private fun BooksAppContent(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    searchWidgetState: BooksViewModel.SearchWidgetState,
    searchTextState: String,
    onTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchClicked: (String) -> Unit,
    onSearchTriggered: () -> Unit,
    onFavoritesClicked: () -> Unit,
    onAiAssistantClicked: () -> Unit,
    booksUiState: BooksUiState,
    homeUiState: HomeUiState,
    categoryUiState: CategoryShelfUiState,
    isSearchActive: Boolean,
    onCategorySelected: (BookCategory) -> Unit,
    onHomeRetry: () -> Unit,
    searchRetryAction: () -> Unit,
    onBookClicked: (Book) -> Unit,
    onFavoriteClicked: (Book) -> Unit,
    onBrowseRequested: (title: String, query: String, orderBy: String?, filter: String?) -> Unit,
    onContinueReadingClicked: (Long) -> Unit,
    onStreakClicked: () -> Unit,
    onPremiumClicked: () -> Unit,
    shouldPlayAiFabIntro: Boolean,
    onAiFabIntroFinished: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!isSearchActive) {
                ExtendedFloatingActionButton(
                    onClick = onAiAssistantClicked,
                    modifier = Modifier.height(52.dp),
                    icon = {
                        AiFabIntroIcon(
                            playIntro = shouldPlayAiFabIntro,
                            onIntroFinished = onAiFabIntroFinished
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(id = R.string.home_ai_fab_title),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        },
        topBar = {
            MainAppBar(
                searchWidgetState = searchWidgetState,
                searchTextState = searchTextState,
                onTextChange = onTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onSearchTriggered = onSearchTriggered,
                onFavoritesClicked = onFavoritesClicked,
                onPremiumClicked = onPremiumClicked
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            // Bridge M3 container with M2 content color so legacy M2 text is readable in dark mode.
            Material2Surface(
                modifier = Modifier.fillMaxSize(),
                color = Material2Theme.colors.background,
                contentColor = Material2Theme.colors.onBackground
            ) {
                HomeScreen(
                    homeUiState = homeUiState,
                    categoryShelfUiState = categoryUiState,
                    booksUiState = booksUiState,
                    isSearchActive = isSearchActive,
                    retryAction = onHomeRetry,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier.fillMaxSize(),
                    onBookClicked = onBookClicked,
                    onFavoriteClicked = onFavoriteClicked,
                    onSearchRetry = searchRetryAction,
                    onBrowseRequested = onBrowseRequested,
                    onAiAssistantClicked = onAiAssistantClicked,
                    onContinueReadingClicked = onContinueReadingClicked,
                    onStreakClicked = onStreakClicked
                )
            }
        }
    }
}

@Composable
private fun AiFabIntroIcon(
    playIntro: Boolean,
    onIntroFinished: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.favorite_burst)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = playIntro && composition != null,
        iterations = 1,
        restartOnPlay = false
    )

    LaunchedEffect(playIntro, composition, progress) {
        if (playIntro && composition != null && progress >= 0.999f) {
            onIntroFinished()
        }
    }

    if (playIntro && composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(20.dp)
                .clip(MaterialTheme.shapes.small)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BooksAppContentPreview() {
    BooksAppContent(
        snackbarHostState = SnackbarHostState(),
        searchWidgetState = BooksViewModel.SearchWidgetState.CLOSED,
        searchTextState = "",
        onTextChange = {},
        onCloseClicked = {},
        onSearchClicked = {},
        onSearchTriggered = {},
        onFavoritesClicked = {},
        onAiAssistantClicked = {},
        booksUiState = BooksUiState.Success(bookSearch = emptyList()),
        homeUiState = HomeUiState(
            isLoading = false,
            featured = emptyList(),
            sections = emptyList(),
            categories = emptyList()
        ),
        categoryUiState = CategoryShelfUiState(),
        isSearchActive = false,
        onCategorySelected = {},
        onHomeRetry = {},
        searchRetryAction = {},
        onBookClicked = {},
        onFavoriteClicked = {},
        onBrowseRequested = { _, _, _, _ -> },
        onContinueReadingClicked = {},
        onStreakClicked = {},
        onPremiumClicked = {},
        shouldPlayAiFabIntro = false,
        onAiFabIntroFinished = {}
    )
}

private data class TopLevelDestination(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)
