package com.partitionsoft.bookshelf.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.GestureDetector
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.data.reader.EpubParser
import com.partitionsoft.bookshelf.data.reader.Fb2Parser
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import com.partitionsoft.bookshelf.domain.model.ReaderDocumentFormat
import com.partitionsoft.bookshelf.ui.LocalReaderUiState
import com.partitionsoft.bookshelf.ui.LocalReaderViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

@Composable
fun LocalReaderRoute(
    onBackClicked: () -> Unit,
    viewModel: LocalReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReadingSessionLifecycleEffect(
        enabled = state is LocalReaderUiState.Ready,
        onSessionStart = viewModel::onReadingSessionStart,
        onSessionStop = viewModel::onReadingSessionStop
    )

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    onProgress = viewModel::updateProgress,
                    showControls = !isFullscreen
                )
                ReaderDocumentFormat.FB2 -> Fb2ReaderContent(
                    document = uiState.document,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    onProgress = viewModel::updateProgress,
                    showControls = !isFullscreen
                )
                ReaderDocumentFormat.UNKNOWN -> UnsupportedLocalFormat(modifier = Modifier.fillMaxSize().padding(paddingValues))
            }
        }
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
    modifier: Modifier = Modifier,
    onProgress: (String) -> Unit,
    showControls: Boolean
) {
    val context = LocalContext.current
    val publicationResult by produceState<Result<EpubParser.Publication>?>(initialValue = null, document.uri) {
        value = EpubParser.parse(context.contentResolver, document.uri.toUri())
    }

    val publication = publicationResult?.getOrNull()
    var chapterIndex by remember { mutableIntStateOf(document.lastLocation?.toIntOrNull() ?: 0) }
    var textZoom by rememberSaveable(document.uri) { mutableIntStateOf(110) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(chapterIndex, publication?.chapters?.size) {
        if (publication != null && publication.chapters.isNotEmpty()) {
            val clamped = chapterIndex.coerceIn(0, publication.chapters.lastIndex)
            if (clamped != chapterIndex) chapterIndex = clamped
            onProgress(chapterIndex.toString())
        }
    }

    when {
        publicationResult == null -> {
            LoadingScreen(modifier = modifier)
            return
        }
        publication == null || publication.chapters.isEmpty() -> {
            UnsupportedLocalFormat(modifier = modifier)
            return
        }
    }

    val chapter = publication.chapters[chapterIndex]
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
        key(document.uri, chapterIndex, isDarkTheme) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(androidx.compose.ui.graphics.Color.White),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.textZoom = textZoom
                        webViewClient = WebViewClient()
                        installHorizontalPageFlingNavigation(
                            onSwipeLeft = {
                                chapterIndex = (chapterIndex + 1).coerceAtMost(publication.chapters.lastIndex)
                            },
                            onSwipeRight = {
                                chapterIndex = (chapterIndex - 1).coerceAtLeast(0)
                            }
                        )
                    }
                },
                update = { webView ->
                    webView.settings.textZoom = textZoom
                    val contentKey = "${document.uri}-${chapterIndex}-${isDarkTheme}"
                    webView.setBackgroundColor(if (isDarkTheme) Color.parseColor("#111827") else Color.WHITE)
                    if (webView.tag != contentKey) {
                        val html = buildReadableEpubHtml(chapter.html, isDarkTheme)
                        webView.tag = contentKey
                        webView.loadDataWithBaseURL(
                            "https://bookshelf.local/epub/${document.id}/$chapterIndex/",
                            html,
                            "text/html",
                            "utf-8",
                            null
                        )
                        webView.post { webView.scrollTo(0, 0) }
                    }
                }
            )
        }
        if (showControls) {
            ZoomControls(
                valueLabel = stringResource(id = R.string.reader_text_size, textZoom),
                onDecrease = { textZoom = (textZoom - 10).coerceAtLeast(80) },
                onIncrease = { textZoom = (textZoom + 10).coerceAtMost(200) },
                onReset = { textZoom = 110 },
                canDecrease = textZoom > 80,
                canIncrease = textZoom < 200
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
    modifier: Modifier = Modifier,
    onProgress: (String) -> Unit,
    showControls: Boolean
) {
    val context = LocalContext.current
    val publicationResult by produceState<Result<Fb2Parser.Publication>?>(initialValue = null, document.uri) {
        value = Fb2Parser.parse(context.contentResolver, document.uri.toUri())
    }

    val publication = publicationResult?.getOrNull()
    var chapterIndex by remember { mutableIntStateOf(document.lastLocation?.toIntOrNull() ?: 0) }
    var textZoom by rememberSaveable(document.uri) { mutableIntStateOf(110) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(chapterIndex, publication?.chapters?.size) {
        if (publication != null && publication.chapters.isNotEmpty()) {
            val clamped = chapterIndex.coerceIn(0, publication.chapters.lastIndex)
            if (clamped != chapterIndex) chapterIndex = clamped
            onProgress(chapterIndex.toString())
        }
    }

    when {
        publicationResult == null -> {
            LoadingScreen(modifier = modifier)
            return
        }
        publication == null || publication.chapters.isEmpty() -> {
            UnsupportedLocalFormat(modifier = modifier)
            return
        }
    }

    val chapter = publication.chapters[chapterIndex]
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
        key(document.uri, chapterIndex, isDarkTheme) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(androidx.compose.ui.graphics.Color.White),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.textZoom = textZoom
                        webViewClient = WebViewClient()
                        installHorizontalPageFlingNavigation(
                            onSwipeLeft = {
                                chapterIndex = (chapterIndex + 1).coerceAtMost(publication.chapters.lastIndex)
                            },
                            onSwipeRight = {
                                chapterIndex = (chapterIndex - 1).coerceAtLeast(0)
                            }
                        )
                    }
                },
                update = { webView ->
                    webView.settings.textZoom = textZoom
                    val contentKey = "${document.uri}-${chapterIndex}-${isDarkTheme}"
                    webView.setBackgroundColor(if (isDarkTheme) Color.parseColor("#111827") else Color.WHITE)
                    if (webView.tag != contentKey) {
                        val html = buildReadableEpubHtml(chapter.html, isDarkTheme)
                        webView.tag = contentKey
                        webView.loadDataWithBaseURL(
                            "https://bookshelf.local/fb2/${document.id}/$chapterIndex/",
                            html,
                            "text/html",
                            "utf-8",
                            null
                        )
                        webView.post { webView.scrollTo(0, 0) }
                    }
                }
            )
        }
        if (showControls) {
            ZoomControls(
                valueLabel = stringResource(id = R.string.reader_text_size, textZoom),
                onDecrease = { textZoom = (textZoom - 10).coerceAtLeast(80) },
                onIncrease = { textZoom = (textZoom + 10).coerceAtMost(200) },
                onReset = { textZoom = 110 },
                canDecrease = textZoom > 80,
                canIncrease = textZoom < 200
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

private fun buildReadableEpubHtml(rawHtml: String, isDarkTheme: Boolean): String {
    val normalizedHtml = rawHtml
        .replace(Regex("(<br\\s*/?>\\s*){3,}", RegexOption.IGNORE_CASE), "<br><br>")
        .replace(Regex("<p>\\s*</p>", RegexOption.IGNORE_CASE), "")

    val bg = if (isDarkTheme) "#0F172A" else "#FFFFFF"
    val fg = if (isDarkTheme) "#E5E7EB" else "#111827"
    val muted = if (isDarkTheme) "#94A3B8" else "#4B5563"

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
                    background: $bg !important;
                    color: $fg !important;
                    font-size: 1rem;
                    line-height: 1.6;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    word-break: break-word;
                }
                body {
                    max-width: 760px;
                    margin: 0 auto;
                    padding: 16px 14px 24px 14px;
                }
                * {
                    max-width: 100% !important;
                }
                p {
                    margin: 0 0 0.95em 0;
                    text-align: start;
                    color: $fg !important;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin: 0 0 0.55em 0;
                    line-height: 1.3;
                    color: $fg !important;
                }
                blockquote {
                    margin: 0 0 1em 0;
                    padding-left: 12px;
                    border-left: 3px solid $muted;
                    color: $muted !important;
                }
                img, svg {
                    max-width: 100% !important;
                    height: auto !important;
                }
                a {
                    color: $muted !important;
                }
                pre, code {
                    white-space: pre-wrap;
                    color: $fg !important;
                }
            </style>
        </head>
        <body>
            $normalizedHtml
        </body>
        </html>
    """.trimIndent()
}
