package com.partitionsoft.bookshelf.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.partitionsoft.bookshelf.domain.model.ReaderBookmark
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import com.partitionsoft.bookshelf.domain.model.ReaderDocumentFormat
import com.partitionsoft.bookshelf.domain.model.ReadingSessionRecord
import com.partitionsoft.bookshelf.domain.repository.ReadingStatsRepository
import com.partitionsoft.bookshelf.domain.repository.ReaderRepository
import com.partitionsoft.bookshelf.ui.navigation.BooksDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LocalReaderUiState {
    data object Loading : LocalReaderUiState
    data class Ready(val document: ReaderDocument) : LocalReaderUiState
    data object Unsupported : LocalReaderUiState
    data object Error : LocalReaderUiState
}

@HiltViewModel
class LocalReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readerRepository: ReaderRepository,
    private val readingStatsRepository: ReadingStatsRepository
) : ViewModel() {

    private val documentId: Long? = savedStateHandle[BooksDestinations.DOCUMENT_ID_ARG]

    private val _uiState = MutableStateFlow<LocalReaderUiState>(LocalReaderUiState.Loading)
    val uiState: StateFlow<LocalReaderUiState> = _uiState.asStateFlow()

    val bookmarks: StateFlow<List<ReaderBookmark>> = (
        documentId?.let { readerRepository.observeBookmarks(it) }
            ?: emptyFlow()
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var sessionStartedAtMillis: Long? = null
    private var activeDocument: ReaderDocument? = null
    private var pagesReachedInSession: Int = 0

    init {
        loadDocument()
    }

    fun loadDocument() {
        viewModelScope.launch {
            val safeDocumentId = documentId
            if (safeDocumentId == null) {
                _uiState.value = LocalReaderUiState.Error
                return@launch
            }
            val document = readerRepository.getDocument(safeDocumentId)
            _uiState.value = when {
                document == null -> LocalReaderUiState.Error
                document.format == ReaderDocumentFormat.UNKNOWN -> LocalReaderUiState.Unsupported
                else -> LocalReaderUiState.Ready(document)
            }
        }
    }

    fun updateProgress(location: String) {
        val reached = location.chapterIndexOrNull()?.plus(1) ?: 0
        pagesReachedInSession = maxOf(pagesReachedInSession, reached)
        viewModelScope.launch {
            val safeDocumentId = documentId ?: return@launch
            readerRepository.updateProgress(safeDocumentId, location)
        }
    }

    fun addBookmark(chapterIndex: Int, scrollY: Int, title: String) {
        viewModelScope.launch {
            val safeDocumentId = documentId ?: return@launch
            readerRepository.addBookmark(safeDocumentId, chapterIndex, scrollY, title)
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            readerRepository.deleteBookmark(bookmarkId)
        }
    }

    fun onReadingSessionStart() {
        if (sessionStartedAtMillis != null) return
        val document = (uiState.value as? LocalReaderUiState.Ready)?.document ?: return
        activeDocument = document
        pagesReachedInSession = document.lastLocation?.chapterIndexOrNull()?.plus(1)?.coerceAtLeast(0) ?: 0
        sessionStartedAtMillis = System.currentTimeMillis()
    }

    fun onReadingSessionStop() {
        val startedAt = sessionStartedAtMillis ?: return
        val document = activeDocument ?: (uiState.value as? LocalReaderUiState.Ready)?.document ?: return
        sessionStartedAtMillis = null
        activeDocument = null

        val endedAt = System.currentTimeMillis()
        val durationSeconds = ((endedAt - startedAt) / 1000L).coerceAtLeast(1L)
        viewModelScope.launch {
            readingStatsRepository.recordReadingSession(
                ReadingSessionRecord(
                    bookRef = "local:${document.id}",
                    bookTitle = document.title,
                    pagesReached = pagesReachedInSession,
                    durationSeconds = durationSeconds,
                    startedAtMillis = startedAt,
                    endedAtMillis = endedAt
                )
            )
        }
        pagesReachedInSession = 0
    }

    private fun String.chapterIndexOrNull(): Int? = substringBefore(':').toIntOrNull()
}
