package com.partitionsoft.bookshelf.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.GestureDetector
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.annotation.Keep
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.data.reader.EpubParser
import com.partitionsoft.bookshelf.data.reader.Fb2Parser
import com.partitionsoft.bookshelf.domain.model.ReaderBookmark
import com.partitionsoft.bookshelf.domain.model.ReaderAnnotation
import com.partitionsoft.bookshelf.domain.model.ReaderAnnotationType
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import com.partitionsoft.bookshelf.domain.model.ReaderDocumentFormat
import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontFamily
import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontWeight
import com.partitionsoft.bookshelf.domain.reader.model.ReaderPageMargin
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSpacing
import com.partitionsoft.bookshelf.domain.reader.model.ReaderTheme
import com.partitionsoft.bookshelf.domain.subscription.model.PremiumFeature
import com.partitionsoft.bookshelf.ui.LocalReaderUiState
import com.partitionsoft.bookshelf.ui.LocalReaderViewModel
import com.partitionsoft.bookshelf.ui.premium.PremiumUpgradeDialog
import com.partitionsoft.bookshelf.ui.reader.ReaderSettingsSheet
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap
import org.json.JSONObject

@Composable
fun LocalReaderRoute(
    onBackClicked: () -> Unit,
    onPremiumRequested: () -> Unit,
    viewModel: LocalReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val annotations by viewModel.annotations.collectAsStateWithLifecycle()
    val customization by viewModel.customization.collectAsStateWithLifecycle()

    ReadingSessionLifecycleEffect(
        enabled = state is LocalReaderUiState.Ready,
        onSessionStart = viewModel::onReadingSessionStart,
        onSessionStop = viewModel::onReadingSessionStop
    )

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showReaderSettings by rememberSaveable { mutableStateOf(false) }
    var showPremiumDialog by rememberSaveable { mutableStateOf(false) }
    val onToggleFullscreen = { isFullscreen = !isFullscreen }

    ReaderImmersiveEffect(isEnabled = isFullscreen)

    Scaffold(
        floatingActionButton = {
            if (isFullscreen) {
                FloatingActionButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = Icons.Filled.FullscreenExit,
                        contentDescription = stringResource(id = R.string.reader_exit_fullscreen)
                    )
                }
            }
        },
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = { Text(text = stringResource(id = R.string.reader_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    },
                    actions = {
                        val supportsTextCustomization = (state as? LocalReaderUiState.Ready)?.document?.format in
                            setOf(ReaderDocumentFormat.EPUB, ReaderDocumentFormat.FB2)
                        if (supportsTextCustomization) {
                            IconButton(onClick = { showReaderSettings = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = stringResource(id = R.string.reader_settings_open)
                                )
                            }
                        }
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = stringResource(id = R.string.reader_enter_fullscreen)
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        when (val uiState = state) {
            LocalReaderUiState.Loading -> LoadingScreen(modifier = Modifier.fillMaxSize().padding(paddingValues))
            LocalReaderUiState.Error -> ErrorScreen(
                retryAction = viewModel::loadDocument,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
            LocalReaderUiState.Unsupported -> UnsupportedLocalFormat(modifier = Modifier.fillMaxSize().padding(paddingValues))
            is LocalReaderUiState.Ready -> when (uiState.document.format) {
                ReaderDocumentFormat.PDF -> PdfReaderContent(
                    document = uiState.document,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    onProgress = viewModel::updateProgress,
                    showControls = !isFullscreen
                )
                ReaderDocumentFormat.EPUB -> EpubPlaceholder(
                    document = uiState.document,
                    bookmarks = bookmarks,
                    annotations = annotations,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    onProgress = viewModel::updateProgress,
                    onAddBookmark = viewModel::addBookmark,
                    onDeleteBookmark = viewModel::deleteBookmark,
                    onAddHighlight = viewModel::addHighlight,
                    onAddNote = viewModel::addNote,
                    onDeleteAnnotation = viewModel::deleteAnnotation,
                    isPremium = customization.isPremium,
                    onPremiumRequired = { showPremiumDialog = true },
                    readerSettings = customization.settings,
                    showControls = !isFullscreen
                )
                ReaderDocumentFormat.FB2 -> Fb2ReaderContent(
                    document = uiState.document,
                    bookmarks = bookmarks,
                    annotations = annotations,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    onProgress = viewModel::updateProgress,
                    onAddBookmark = viewModel::addBookmark,
                    onDeleteBookmark = viewModel::deleteBookmark,
                    onAddHighlight = viewModel::addHighlight,
                    onAddNote = viewModel::addNote,
                    onDeleteAnnotation = viewModel::deleteAnnotation,
                    isPremium = customization.isPremium,
                    onPremiumRequired = { showPremiumDialog = true },
                    readerSettings = customization.settings,
                    showControls = !isFullscreen
                )
                ReaderDocumentFormat.UNKNOWN -> UnsupportedLocalFormat(modifier = Modifier.fillMaxSize().padding(paddingValues))
            }
        }
    }

    if (showReaderSettings) {
        ReaderSettingsSheet(
            settings = customization.settings,
            isPremium = customization.isPremium,
            onSettingsChanged = viewModel::updateReaderSettings,
            onReset = viewModel::resetReaderSettings,
            onPremiumRequired = { showPremiumDialog = true },
            onDismiss = { showReaderSettings = false }
        )
    }

    if (showPremiumDialog) {
        PremiumUpgradeDialog(
            feature = PremiumFeature.ADVANCED_READER_CUSTOMIZATION,
            onDismiss = { showPremiumDialog = false },
            onViewPremium = {
                showPremiumDialog = false
                showReaderSettings = false
                onPremiumRequested()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfReaderContent(
    document: ReaderDocument,
    modifier: Modifier = Modifier,
    onProgress: (String) -> Unit,
    showControls: Boolean
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }
    var pageIndex by remember { mutableIntStateOf(document.lastLocation?.toIntOrNull() ?: 0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomPercent by rememberSaveable(document.uri) { mutableIntStateOf(100) }
    val minPdfZoom = 70
    val maxPdfZoom = 220
    val isPanMode = zoomPercent > 100
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val pdfTransformableState = rememberTransformableState { zoomChange, _, _ ->
        if (zoomChange == 1f) return@rememberTransformableState
        val nextZoom = (zoomPercent * zoomChange)
            .roundToInt()
            .coerceIn(minPdfZoom, maxPdfZoom)
        if (abs(nextZoom - zoomPercent) >= 1) {
            zoomPercent = nextZoom
        }
    }

    val rendererHolder = remember(document.uri) {
        runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(document.uri.toUri(), "r")
                ?: error("Unable to open PDF")
            val renderer = PdfRenderer(descriptor)
            PdfRendererHolder(descriptor, renderer)
        }.getOrNull()
    }

    DisposableEffect(rendererHolder, pageIndex, viewportSize, zoomPercent) {
        if (rendererHolder != null) {
            pageCount = rendererHolder.renderer.pageCount
            val safePage = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            if (safePage != pageIndex) pageIndex = safePage
            if (pageCount > 0) {
                val page = rendererHolder.renderer.openPage(pageIndex)

                val pageWidth = page.width.coerceAtLeast(1)
                val pageHeight = page.height.coerceAtLeast(1)
                val targetWidthBase = if (viewportSize.width > 0) {
                    viewportSize.width
                } else {
                    pageWidth
                }
                val scaledWidth = (targetWidthBase * (zoomPercent / 100f)).toInt().coerceAtLeast(1)
                val aspectRatio = pageHeight.toFloat() / pageWidth.toFloat()
                val scaledHeight = (scaledWidth * aspectRatio).toInt().coerceAtLeast(1)
                val (targetWidth, targetHeight) = clampBitmapSize(
                    width = scaledWidth,
                    height = scaledHeight,
                    maxSidePx = 4096,
                    maxPixels = 8_000_000
                )

                val rendered = createBitmap(targetWidth, targetHeight)
                // Many PDFs draw transparent background; force white page for readable text.
                rendered.eraseColor(Color.WHITE)
                page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap = rendered
                onProgress(pageIndex.toString())
            }
        }
        onDispose { }
    }

    DisposableEffect(rendererHolder) {
        onDispose {
            rendererHolder?.close()
        }
    }

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (bitmap == null) {
            Text(text = stringResource(id = R.string.loading), style = MaterialTheme.typography.body2)
        } else {
            val currentBitmap = bitmap!!
            val imageWidthDp = with(density) { currentBitmap.width.toDp() }
            val imageHeightDp = with(density) { currentBitmap.height.toDp() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color.White)
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(pageIndex, pageCount, zoomPercent) {
                        var dragAmountX = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                if (zoomPercent <= 100) {
                                    dragAmountX += dragAmount
                                }
                            },
                            onDragEnd = {
                                if (zoomPercent <= 100 && abs(dragAmountX) >= swipeThresholdPx) {
                                    val direction = if (dragAmountX < 0f) 1 else -1
                                    pageIndex = (pageIndex + direction).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                                }
                                dragAmountX = 0f
                            },
                            onDragCancel = { dragAmountX = 0f }
                        )
                    }
                    .transformable(
                        state = pdfTransformableState,
                        canPan = { false }
                    )
                    // Keep panning only for zoomed-in pages so fit-zoom drags flip pages.
                    .horizontalScroll(horizontalScrollState, enabled = isPanMode)
                    .verticalScroll(verticalScrollState, enabled = isPanMode)
            ) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = document.title,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .background(androidx.compose.ui.graphics.Color.White)
                        .padding(4.dp)
                        .requiredSize(width = imageWidthDp, height = imageHeightDp)
                )
            }
        }

        if (showControls && pageCount > 0) {
            Text(
                text = stringResource(id = R.string.page_position, pageIndex + 1, pageCount),
                style = MaterialTheme.typography.caption
            )
            ZoomControls(
                valueLabel = stringResource(id = R.string.reader_pdf_zoom, zoomPercent),
                onDecrease = { zoomPercent = (zoomPercent - 10).coerceAtLeast(minPdfZoom) },
                onIncrease = { zoomPercent = (zoomPercent + 10).coerceAtMost(maxPdfZoom) },
                onReset = { zoomPercent = 100 },
                canDecrease = zoomPercent > minPdfZoom,
                canIncrease = zoomPercent < maxPdfZoom
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                    enabled = pageIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.previous_page))
                }
                Button(
                    onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
                    enabled = pageIndex < pageCount - 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.next_page))
                }
            }
        }
    }
}

@Composable
private fun EpubPlaceholder(
    document: ReaderDocument,
    bookmarks: List<ReaderBookmark>,
    annotations: List<ReaderAnnotation>,
    modifier: Modifier = Modifier,
    onProgress: (String) -> Unit,
    onAddBookmark: (Int, Int, String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onAddHighlight: (Int, Int, String, String) -> Unit,
    onAddNote: (Int, Int, String, String) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    isPremium: Boolean,
    onPremiumRequired: () -> Unit,
    readerSettings: ReaderSettings,
    showControls: Boolean
) {
    val context = LocalContext.current
    val publicationResult by produceState<Result<EpubParser.Publication>?>(initialValue = null, document.uri) {
        value = EpubParser.parse(context.contentResolver, document.uri.toUri())
    }

    val publication = publicationResult?.getOrNull()
    var chapterIndex by remember { mutableIntStateOf(document.lastLocation.chapterIndexOrDefault()) }
    var currentScrollY by remember { mutableIntStateOf(document.lastLocation.scrollYOrDefault()) }
    var pendingScrollY by remember { mutableStateOf<Int?>(document.lastLocation.scrollYOrDefault()) }
    val isSystemDark = isSystemInDarkTheme()

    LaunchedEffect(chapterIndex, publication?.chapters?.size) {
        if (publication != null && publication.chapters.isNotEmpty()) {
            val clamped = chapterIndex.coerceIn(0, publication.chapters.lastIndex)
            if (clamped != chapterIndex) chapterIndex = clamped
            onProgress(formatTextLocation(chapterIndex, currentScrollY))
        }
    }

    when {
        publicationResult == null -> {
            LoadingScreen(modifier = modifier)
            return
        }
        publicationResult?.isFailure == true -> {
            Log.e(READER_LOG_TAG, "Unable to open EPUB ${document.uri}", publicationResult?.exceptionOrNull())
            UnreadableLocalFile(modifier = modifier)
            return
        }
        publication == null || publication.chapters.isEmpty() -> {
            UnsupportedLocalFormat(modifier = modifier)
            return
        }
    }

    val chapter = publication.chapters[chapterIndex]
    val chapterAnnotations = annotations.filter { isPremium && it.chapterIndex == chapterIndex }
    val bookTitle = publication.title
        .takeUnless { it.isBlank() || it.looksLikeTechnicalId() }
        ?: stringResource(id = R.string.epub_book_fallback_title)
    val chapterTitle = chapter.title
        .takeUnless { it.isBlank() || it.endsWith(".html", ignoreCase = true) || it.endsWith(".xhtml", ignoreCase = true) }
        ?: stringResource(id = R.string.epub_chapter_fallback_title, chapterIndex + 1)

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showControls) {
            Text(
                text = bookTitle,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        key(document.uri, chapterIndex) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        androidx.compose.ui.graphics.Color(
                            Color.parseColor(readerSettings.backgroundColor(isSystemDark))
                        )
                    ),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.textZoom = readerSettings.fontSizePercent
                        webViewClient = WebViewClient()
                        addJavascriptInterface(
                            ReaderAnnotationBridge(
                                isPremium = isPremium,
                                onHighlight = { text, color, scrollY ->
                                    post { onAddHighlight(chapterIndex, scrollY, text, color) }
                                },
                                onPremiumRequired = { post(onPremiumRequired) }
                            ),
                            READER_ANNOTATION_BRIDGE
                        )
                        setOnScrollChangeListener { _, _, scrollY, _, _ ->
                            if (scrollY != currentScrollY) {
                                currentScrollY = scrollY.coerceAtLeast(0)
                                onProgress(formatTextLocation(chapterIndex, currentScrollY))
                            }
                        }
                        installHorizontalPageFlingNavigation(
                            onSwipeLeft = { chapterIndex = (chapterIndex + 1).coerceAtMost(publication.chapters.lastIndex) },
                            onSwipeRight = { chapterIndex = (chapterIndex - 1).coerceAtLeast(0) }
                        )
                    }
                },
                update = { webView ->
                    webView.settings.textZoom = readerSettings.fontSizePercent
                    val contentKey = "${document.uri}-${chapterIndex}-${readerSettings}-${isSystemDark}-${chapterAnnotations.annotationKey()}"
                    webView.setBackgroundColor(Color.parseColor(readerSettings.backgroundColor(isSystemDark)))
                    if (webView.tag != contentKey) {
                        val html = buildReadableEpubHtml(chapter.html, readerSettings, isSystemDark, chapterAnnotations)
                        webView.tag = contentKey
                        webView.loadDataWithBaseURL(
                            "https://bookshelf.local/epub/${document.id}/$chapterIndex/",
                            html,
                            "text/html",
                            "utf-8",
                            null
                        )
                        val scrollY = pendingScrollY ?: currentScrollY
                        pendingScrollY = null
                        webView.post { webView.scrollTo(0, scrollY) }
                    }
                    pendingScrollY?.let { scrollY ->
                        pendingScrollY = null
                        webView.post { webView.scrollTo(0, scrollY) }
                    }
                }
            )
        }
        if (showControls) {
            TextBookmarkControls(
                bookmarks = bookmarks,
                currentChapterIndex = chapterIndex,
                currentScrollY = currentScrollY,
                onAddBookmark = {
                    onAddBookmark(
                        chapterIndex,
                        currentScrollY,
                        chapterTitle
                    )
                },
                onOpenBookmark = { bookmark ->
                    chapterIndex = bookmark.chapterIndex.coerceIn(0, publication.chapters.lastIndex)
                    currentScrollY = bookmark.scrollY
                    pendingScrollY = bookmark.scrollY
                },
                onDeleteBookmark = onDeleteBookmark
            )
            ReaderAnnotationControls(
                annotations = annotations,
                currentChapterIndex = chapterIndex,
                currentScrollY = currentScrollY,
                isPremium = isPremium,
                onAddNote = { note -> onAddNote(chapterIndex, currentScrollY, "", note) },
                onOpenAnnotation = { annotation ->
                    chapterIndex = annotation.chapterIndex.coerceIn(0, publication.chapters.lastIndex)
                    currentScrollY = annotation.scrollY
                    pendingScrollY = annotation.scrollY
                },
                onDeleteAnnotation = onDeleteAnnotation,
                onPremiumRequired = onPremiumRequired
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { chapterIndex = (chapterIndex - 1).coerceAtLeast(0) },
                    enabled = chapterIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = stringResource(id = R.string.previous_page)
                    )
                }
                Text(
                    text = stringResource(id = R.string.chapter_position, chapterIndex + 1, publication.chapters.size),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 14.dp)
                )
                IconButton(
                    onClick = { chapterIndex = (chapterIndex + 1).coerceAtMost(publication.chapters.lastIndex) },
                    enabled = chapterIndex < publication.chapters.lastIndex
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(id = R.string.next_page)
                    )
                }
            }
        }
    }
}

@Composable
private fun Fb2ReaderContent(
    document: ReaderDocument,
    bookmarks: List<ReaderBookmark>,
    annotations: List<ReaderAnnotation>,
    modifier: Modifier = Modifier,
    onProgress: (String) -> Unit,
    onAddBookmark: (Int, Int, String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onAddHighlight: (Int, Int, String, String) -> Unit,
    onAddNote: (Int, Int, String, String) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    isPremium: Boolean,
    onPremiumRequired: () -> Unit,
    readerSettings: ReaderSettings,
    showControls: Boolean
) {
    val context = LocalContext.current
    val publicationResult by produceState<Result<Fb2Parser.Publication>?>(initialValue = null, document.uri) {
        value = Fb2Parser.parse(context.contentResolver, document.uri.toUri())
    }

    val publication = publicationResult?.getOrNull()
    var chapterIndex by remember { mutableIntStateOf(document.lastLocation.chapterIndexOrDefault()) }
    var currentScrollY by remember { mutableIntStateOf(document.lastLocation.scrollYOrDefault()) }
    var pendingScrollY by remember { mutableStateOf<Int?>(document.lastLocation.scrollYOrDefault()) }
    val isSystemDark = isSystemInDarkTheme()

    LaunchedEffect(chapterIndex, publication?.chapters?.size) {
        if (publication != null && publication.chapters.isNotEmpty()) {
            val clamped = chapterIndex.coerceIn(0, publication.chapters.lastIndex)
            if (clamped != chapterIndex) chapterIndex = clamped
            onProgress(formatTextLocation(chapterIndex, currentScrollY))
        }
    }

    when {
        publicationResult == null -> {
            LoadingScreen(modifier = modifier)
            return
        }
        publicationResult?.isFailure == true -> {
            Log.e(READER_LOG_TAG, "Unable to open FB2 ${document.uri}", publicationResult?.exceptionOrNull())
            UnreadableLocalFile(modifier = modifier)
            return
        }
        publication == null || publication.chapters.isEmpty() -> {
            UnsupportedLocalFormat(modifier = modifier)
            return
        }
    }

    val chapter = publication.chapters[chapterIndex]
    val chapterAnnotations = annotations.filter { isPremium && it.chapterIndex == chapterIndex }
    val bookTitle = publication.title
        .takeUnless { it.isBlank() || it.looksLikeTechnicalId() }
        ?: stringResource(id = R.string.fb2_book_fallback_title)
    val chapterTitle = chapter.title
        .takeUnless { it.isBlank() }
        ?: stringResource(id = R.string.fb2_chapter_fallback_title, chapterIndex + 1)

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showControls) {
            Text(
                text = bookTitle,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        key(document.uri, chapterIndex) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        androidx.compose.ui.graphics.Color(
                            Color.parseColor(readerSettings.backgroundColor(isSystemDark))
                        )
                    ),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.textZoom = readerSettings.fontSizePercent
                        webViewClient = WebViewClient()
                        addJavascriptInterface(
                            ReaderAnnotationBridge(
                                isPremium = isPremium,
                                onHighlight = { text, color, scrollY ->
                                    post { onAddHighlight(chapterIndex, scrollY, text, color) }
                                },
                                onPremiumRequired = { post(onPremiumRequired) }
                            ),
                            READER_ANNOTATION_BRIDGE
                        )
                        setOnScrollChangeListener { _, _, scrollY, _, _ ->
                            if (scrollY != currentScrollY) {
                                currentScrollY = scrollY.coerceAtLeast(0)
                                onProgress(formatTextLocation(chapterIndex, currentScrollY))
                            }
                        }
                        installHorizontalPageFlingNavigation(
                            onSwipeLeft = { chapterIndex = (chapterIndex + 1).coerceAtMost(publication.chapters.lastIndex) },
                            onSwipeRight = { chapterIndex = (chapterIndex - 1).coerceAtLeast(0) }
                        )
                    }
                },
                update = { webView ->
                    webView.settings.textZoom = readerSettings.fontSizePercent
                    val contentKey = "${document.uri}-${chapterIndex}-${readerSettings}-${isSystemDark}-${chapterAnnotations.annotationKey()}"
                    webView.setBackgroundColor(Color.parseColor(readerSettings.backgroundColor(isSystemDark)))
                    if (webView.tag != contentKey) {
                        val html = buildReadableEpubHtml(chapter.html, readerSettings, isSystemDark, chapterAnnotations)
                        webView.tag = contentKey
                        webView.loadDataWithBaseURL(
                            "https://bookshelf.local/fb2/${document.id}/$chapterIndex/",
                            html,
                            "text/html",
                            "utf-8",
                            null
                        )
                        val scrollY = pendingScrollY ?: currentScrollY
                        pendingScrollY = null
                        webView.post { webView.scrollTo(0, scrollY) }
                    }
                    pendingScrollY?.let { scrollY ->
                        pendingScrollY = null
                        webView.post { webView.scrollTo(0, scrollY) }
                    }
                }
            )
        }
        if (showControls) {
            TextBookmarkControls(
                bookmarks = bookmarks,
                currentChapterIndex = chapterIndex,
                currentScrollY = currentScrollY,
                onAddBookmark = {
                    onAddBookmark(
                        chapterIndex,
                        currentScrollY,
                        chapterTitle
                    )
                },
                onOpenBookmark = { bookmark ->
                    chapterIndex = bookmark.chapterIndex.coerceIn(0, publication.chapters.lastIndex)
                    currentScrollY = bookmark.scrollY
                    pendingScrollY = bookmark.scrollY
                },
                onDeleteBookmark = onDeleteBookmark
            )
            ReaderAnnotationControls(
                annotations = annotations,
                currentChapterIndex = chapterIndex,
                currentScrollY = currentScrollY,
                isPremium = isPremium,
                onAddNote = { note -> onAddNote(chapterIndex, currentScrollY, "", note) },
                onOpenAnnotation = { annotation ->
                    chapterIndex = annotation.chapterIndex.coerceIn(0, publication.chapters.lastIndex)
                    currentScrollY = annotation.scrollY
                    pendingScrollY = annotation.scrollY
                },
                onDeleteAnnotation = onDeleteAnnotation,
                onPremiumRequired = onPremiumRequired
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { chapterIndex = (chapterIndex - 1).coerceAtLeast(0) },
                    enabled = chapterIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = stringResource(id = R.string.previous_page)
                    )
                }
                Text(
                    text = stringResource(id = R.string.chapter_position, chapterIndex + 1, publication.chapters.size),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 14.dp)
                )
                IconButton(
                    onClick = { chapterIndex = (chapterIndex + 1).coerceAtMost(publication.chapters.lastIndex) },
                    enabled = chapterIndex < publication.chapters.lastIndex
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(id = R.string.next_page)
                    )
                }
            }
        }
    }
}

@Composable
private fun TextBookmarkControls(
    bookmarks: List<ReaderBookmark>,
    currentChapterIndex: Int,
    currentScrollY: Int,
    onAddBookmark: () -> Unit,
    onOpenBookmark: (ReaderBookmark) -> Unit,
    onDeleteBookmark: (Long) -> Unit
) {
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val bookmarkAddedMessage = stringResource(R.string.reader_bookmark_added)

    val addBookmarkWithFeedback = {
        onAddBookmark()
        Toast.makeText(
            context,
            bookmarkAddedMessage,
            Toast.LENGTH_SHORT
        ).show()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(
                id = R.string.reader_current_position,
                currentChapterIndex + 1,
                currentScrollY
            ),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(top = 14.dp)
        )
        Row {
            IconButton(onClick = addBookmarkWithFeedback) {
                Icon(
                    imageVector = Icons.Filled.BookmarkAdd,
                    contentDescription = stringResource(id = R.string.reader_add_bookmark)
                )
            }
            IconButton(
                onClick = { showBookmarks = true },
                enabled = bookmarks.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmarks,
                    contentDescription = stringResource(id = R.string.reader_bookmarks)
                )
            }
        }
    }

    if (showBookmarks) {
        AlertDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text(text = stringResource(id = R.string.reader_bookmarks)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (bookmarks.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.reader_bookmarks_empty),
                            style = MaterialTheme.typography.body2
                        )
                    } else {
                        bookmarks.forEachIndexed { index, bookmark ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showBookmarks = false
                                            onOpenBookmark(bookmark)
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.subtitle2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(
                                            id = R.string.reader_bookmark_position,
                                            bookmark.chapterIndex + 1,
                                            bookmark.scrollY
                                        ),
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
                                    )
                                }
                                IconButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(id = R.string.reader_delete_bookmark)
                                    )
                                }
                            }
                            if (index < bookmarks.lastIndex) Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarks = false }) {
                    Text(text = stringResource(id = R.string.close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addBookmarkWithFeedback()
                        showBookmarks = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.reader_add_bookmark))
                }
            }
        )
    }
}

@Composable
private fun ReaderAnnotationControls(
    annotations: List<ReaderAnnotation>,
    currentChapterIndex: Int,
    currentScrollY: Int,
    isPremium: Boolean,
    onAddNote: (String) -> Unit,
    onOpenAnnotation: (ReaderAnnotation) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    onPremiumRequired: () -> Unit
) {
    var showAnnotations by rememberSaveable { mutableStateOf(false) }
    var showAddNote by rememberSaveable { mutableStateOf(false) }
    var noteText by rememberSaveable { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = { if (isPremium) showAddNote = true else onPremiumRequired() }) {
            Icon(Icons.Filled.BorderColor, contentDescription = stringResource(R.string.reader_add_note))
        }
        TextButton(onClick = { if (isPremium) showAnnotations = true else onPremiumRequired() }) {
            Text(stringResource(R.string.reader_annotations_count, annotations.size))
        }
    }

    if (showAddNote) {
        AlertDialog(
            onDismissRequest = { showAddNote = false },
            title = { Text(stringResource(R.string.reader_add_note)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.reader_current_position, currentChapterIndex + 1, currentScrollY))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(stringResource(R.string.reader_note_hint)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = noteText.isNotBlank(),
                    onClick = {
                        onAddNote(noteText)
                        noteText = ""
                        showAddNote = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddNote = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAnnotations) {
        AlertDialog(
            onDismissRequest = { showAnnotations = false },
            title = { Text(stringResource(R.string.reader_annotations)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (annotations.isEmpty()) {
                        Text(stringResource(R.string.reader_annotations_empty))
                    }
                    annotations.forEach { annotation ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showAnnotations = false
                                        onOpenAnnotation(annotation)
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = annotation.noteText.ifBlank { annotation.selectedText },
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    stringResource(
                                        R.string.reader_bookmark_position,
                                        annotation.chapterIndex + 1,
                                        annotation.scrollY
                                    ),
                                    style = MaterialTheme.typography.caption
                                )
                            }
                            IconButton(onClick = { onDeleteAnnotation(annotation.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.reader_delete_annotation))
                            }
                        }
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnnotations = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
private fun UnsupportedLocalFormat(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.reader_unsupported_format),
            style = MaterialTheme.typography.h6
        )
    }
}

@Composable
private fun UnreadableLocalFile(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.reader_file_access_lost),
            style = MaterialTheme.typography.h6
        )
    }
}

@Composable
private fun ReaderImmersiveEffect(isEnabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, isEnabled) {
        val activity = context.findActivity()
        val window = activity?.window
        val decorView = window?.decorView
        if (window == null || decorView == null) {
            return@DisposableEffect onDispose { }
        }

        val controller = WindowInsetsControllerCompat(window, decorView)
        WindowCompat.setDecorFitsSystemWindows(window, !isEnabled)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isEnabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private class PdfRendererHolder(
    private val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
) {
    fun close() {
        renderer.close()
        descriptor.close()
    }
}

private fun String.looksLikeTechnicalId(): Boolean {
    val normalized = trim()
    return normalized.length >= 28 && normalized.all { it.isLetterOrDigit() || it == '-' || it == '_' }
}

private fun String?.chapterIndexOrDefault(): Int =
    this?.substringBefore(':')?.toIntOrNull()?.coerceAtLeast(0) ?: 0

private fun String?.scrollYOrDefault(): Int =
    this?.substringAfter(':', missingDelimiterValue = "0")?.toIntOrNull()?.coerceAtLeast(0) ?: 0

private fun formatTextLocation(chapterIndex: Int, scrollY: Int): String =
    "${chapterIndex.coerceAtLeast(0)}:${scrollY.coerceAtLeast(0)}"

@Composable
private fun ZoomControls(
    valueLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDecrease, enabled = canDecrease) {
            Text(text = "A-")
        }
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(top = 14.dp)
        )
        Row {
            IconButton(onClick = onReset) {
                Text(text = "100%")
            }
            IconButton(onClick = onIncrease, enabled = canIncrease) {
                Text(text = "A+")
            }
        }
    }
}

private fun clampBitmapSize(width: Int, height: Int, maxSidePx: Int, maxPixels: Int): Pair<Int, Int> {
    val sideClampedWidth = width.coerceIn(1, maxSidePx)
    val sideClampedHeight = height.coerceIn(1, maxSidePx)
    val pixelCount = sideClampedWidth.toLong() * sideClampedHeight.toLong()
    if (pixelCount <= maxPixels) {
        return sideClampedWidth to sideClampedHeight
    }

    val scale = kotlin.math.sqrt(maxPixels / pixelCount.toDouble())
    return (
        (sideClampedWidth * scale).toInt().coerceAtLeast(1) to
            (sideClampedHeight * scale).toInt().coerceAtLeast(1)
        )
}

private fun WebView.installHorizontalPageFlingNavigation(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: android.view.MotionEvent?,
            e2: android.view.MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y
            val isHorizontalFling = abs(dx) > abs(dy) * 1.2f
            val isStrongEnough = abs(dx) > 120f && abs(velocityX) > 700f && abs(velocityX) > abs(velocityY)
            if (!isHorizontalFling || !isStrongEnough) return false

            if (dx < 0f) onSwipeLeft() else onSwipeRight()
            return true
        }
    })

    setOnTouchListener { _, event ->
        detector.onTouchEvent(event)
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            performClick()
        }
        false
    }
}

private fun buildReadableEpubHtml(
    rawHtml: String,
    settings: ReaderSettings,
    isSystemDark: Boolean,
    annotations: List<ReaderAnnotation>
): String {
    val normalizedHtml = rawHtml
        .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\son[a-z]+\\s*=\\s*(['\"]).*?\\1", RegexOption.IGNORE_CASE), "")
        .replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
        .replace(Regex("(<br\\s*/?>\\s*){3,}", RegexOption.IGNORE_CASE), "<br><br>")
        .replace(Regex("<p>\\s*</p>", RegexOption.IGNORE_CASE), "")

    val colors = settings.readerColors(isSystemDark)
    val fontFamily = when (settings.fontFamily) {
        ReaderFontFamily.SYSTEM -> "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
        ReaderFontFamily.SERIF -> "Georgia, 'Times New Roman', serif"
        ReaderFontFamily.SANS_SERIF -> "Arial, Helvetica, sans-serif"
        ReaderFontFamily.MONOSPACE -> "'Courier New', monospace"
    }
    val fontWeight = when (settings.fontWeight) {
        ReaderFontWeight.NORMAL -> 400
        ReaderFontWeight.MEDIUM -> 500
        ReaderFontWeight.BOLD -> 700
    }
    val lineHeight = when (settings.lineSpacing) {
        ReaderSpacing.COMPACT -> 1.35
        ReaderSpacing.NORMAL -> 1.6
        ReaderSpacing.RELAXED -> 1.85
    }
    val paragraphSpacing = when (settings.paragraphSpacing) {
        ReaderSpacing.COMPACT -> 0.55
        ReaderSpacing.NORMAL -> 0.95
        ReaderSpacing.RELAXED -> 1.4
    }
    val horizontalPadding = when (settings.pageMargin) {
        ReaderPageMargin.NARROW -> 8
        ReaderPageMargin.NORMAL -> 14
        ReaderPageMargin.WIDE -> 32
    }
    val highlightsJson = annotations
        .filter { it.type == ReaderAnnotationType.HIGHLIGHT && it.selectedText.isNotBlank() }
        .joinToString(prefix = "[", postfix = "]") { annotation ->
            "{text:${JSONObject.quote(annotation.selectedText)},color:${JSONObject.quote(annotation.colorHex)}}"
        }

    return """
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes" />
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: ${colors.background} !important;
                    color: ${colors.foreground} !important;
                    font-size: 1rem;
                    line-height: $lineHeight;
                    font-family: $fontFamily;
                    font-weight: $fontWeight;
                    word-break: break-word;
                }
                body {
                    max-width: 760px;
                    margin: 0 auto;
                    padding: 16px ${horizontalPadding}px 24px ${horizontalPadding}px;
                }
                * {
                    max-width: 100% !important;
                }
                p {
                    margin: 0 0 ${paragraphSpacing}em 0;
                    text-align: start;
                    color: ${colors.foreground} !important;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin: 0 0 0.55em 0;
                    line-height: 1.3;
                    color: ${colors.foreground} !important;
                }
                blockquote {
                    margin: 0 0 1em 0;
                    padding-left: 12px;
                    border-left: 3px solid ${colors.muted};
                    color: ${colors.muted} !important;
                }
                img, svg {
                    max-width: 100% !important;
                    height: auto !important;
                }
                a {
                    color: ${colors.muted} !important;
                }
                pre, code {
                    white-space: pre-wrap;
                    color: ${colors.foreground} !important;
                }
                #inkwell-highlight-tools {
                    position: fixed;
                    left: 50%;
                    bottom: 18px;
                    transform: translateX(-50%);
                    display: none;
                    gap: 10px;
                    padding: 10px 14px;
                    border-radius: 24px;
                    background: rgba(24, 24, 27, 0.94);
                    box-shadow: 0 4px 18px rgba(0,0,0,.3);
                    z-index: 2147483647;
                }
                #inkwell-highlight-tools button {
                    width: 30px;
                    height: 30px;
                    border: 2px solid white;
                    border-radius: 50%;
                }
                mark.inkwell-highlight { color: inherit; border-radius: 3px; padding: 0 1px; }
            </style>
        </head>
        <body>
            $normalizedHtml
            <div id="inkwell-highlight-tools">
                <button data-color="#FFF59D" style="background:#FFF59D"></button>
                <button data-color="#A5D6A7" style="background:#A5D6A7"></button>
                <button data-color="#90CAF9" style="background:#90CAF9"></button>
                <button data-color="#F8BBD0" style="background:#F8BBD0"></button>
            </div>
            <script>
                (() => {
                    const savedHighlights = $highlightsJson;
                    const tools = document.getElementById('inkwell-highlight-tools');
                    let selectedText = '';

                    function applyHighlight(text, color) {
                        if (!text) return;
                        const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
                        const nodes = [];
                        let combined = '';
                        while (walker.nextNode()) {
                            const node = walker.currentNode;
                            if (!node.parentElement || node.parentElement.closest('#inkwell-highlight-tools, mark')) continue;
                            nodes.push({ node, start: combined.length, end: combined.length + node.nodeValue.length });
                            combined += node.nodeValue;
                        }
                        const index = combined.indexOf(text);
                        if (index < 0) return;
                        const endIndex = index + text.length;
                        const startNode = nodes.find(item => index >= item.start && index < item.end);
                        const endNode = nodes.find(item => endIndex > item.start && endIndex <= item.end);
                        if (!startNode || !endNode) return;
                        const range = document.createRange();
                        range.setStart(startNode.node, index - startNode.start);
                        range.setEnd(endNode.node, endIndex - endNode.start);
                        const mark = document.createElement('mark');
                        mark.className = 'inkwell-highlight';
                        mark.style.backgroundColor = color;
                        mark.appendChild(range.extractContents());
                        range.insertNode(mark);
                    }

                    savedHighlights.forEach(item => applyHighlight(item.text, item.color));

                    function updateTools() {
                        const selection = window.getSelection();
                        selectedText = selection ? selection.toString().trim() : '';
                        tools.style.display = selectedText ? 'flex' : 'none';
                    }
                    document.addEventListener('selectionchange', updateTools);
                    document.addEventListener('touchend', () => setTimeout(updateTools, 50));
                    tools.querySelectorAll('button').forEach(button => {
                        button.addEventListener('click', () => {
                            if (selectedText && window.$READER_ANNOTATION_BRIDGE) {
                                window.$READER_ANNOTATION_BRIDGE.addHighlight(
                                    selectedText,
                                    button.dataset.color,
                                    Math.max(0, Math.round(window.scrollY))
                                );
                            }
                            window.getSelection().removeAllRanges();
                            tools.style.display = 'none';
                        });
                    });
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Keep
private class ReaderAnnotationBridge(
    private val isPremium: Boolean,
    private val onHighlight: (String, String, Int) -> Unit,
    private val onPremiumRequired: () -> Unit
) {
    @JavascriptInterface
    fun addHighlight(text: String, color: String, scrollY: Int) {
        if (!isPremium) {
            onPremiumRequired()
            return
        }
        val safeColor = color.takeIf { it in READER_HIGHLIGHT_COLORS } ?: READER_HIGHLIGHT_COLORS.first()
        onHighlight(text.trim().take(2_000), safeColor, scrollY.coerceAtLeast(0))
    }
}

private fun List<ReaderAnnotation>.annotationKey(): String =
    joinToString(separator = ":") { "${it.id}-${it.colorHex}" }

private data class ReaderHtmlColors(
    val background: String,
    val foreground: String,
    val muted: String
)

private fun ReaderSettings.readerColors(isSystemDark: Boolean): ReaderHtmlColors = when (theme) {
    ReaderTheme.SYSTEM -> if (isSystemDark) DARK_READER_COLORS else LIGHT_READER_COLORS
    ReaderTheme.LIGHT -> LIGHT_READER_COLORS
    ReaderTheme.DARK -> DARK_READER_COLORS
    ReaderTheme.SEPIA -> ReaderHtmlColors("#F4ECD8", "#4A3B2A", "#78664F")
    ReaderTheme.AMOLED -> ReaderHtmlColors("#000000", "#F5F5F5", "#A3A3A3")
}

private fun ReaderSettings.backgroundColor(isSystemDark: Boolean): String =
    readerColors(isSystemDark).background

private val LIGHT_READER_COLORS = ReaderHtmlColors("#FFFFFF", "#111827", "#4B5563")
private val DARK_READER_COLORS = ReaderHtmlColors("#0F172A", "#E5E7EB", "#94A3B8")
private const val READER_ANNOTATION_BRIDGE = "InkwellAnnotations"
private val READER_HIGHLIGHT_COLORS = setOf("#FFF59D", "#A5D6A7", "#90CAF9", "#F8BBD0")
private const val READER_LOG_TAG = "InkwellReader"
