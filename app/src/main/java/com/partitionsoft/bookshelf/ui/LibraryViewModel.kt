package com.partitionsoft.bookshelf.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import com.partitionsoft.bookshelf.domain.model.ReaderDocumentFormat
import com.partitionsoft.bookshelf.domain.model.ReadingStatsSnapshot
import com.partitionsoft.bookshelf.domain.repository.ReaderRepository
import com.partitionsoft.bookshelf.domain.repository.ReadingStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingBadge(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val supportingRes: Int
)

sealed interface LibraryFilter {
    val format: ReaderDocumentFormat?

    data object All : LibraryFilter {
        override val format: ReaderDocumentFormat? = null
    }

    data object InProgress : LibraryFilter {
        override val format: ReaderDocumentFormat? = null
    }

    data object Unread : LibraryFilter {
        override val format: ReaderDocumentFormat? = null
    }

    companion object {
        fun defaults(): List<LibraryFilter> = listOf(All, InProgress, Unread)
    }
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val documents: List<ReaderDocument> = emptyList(),
    val filteredDocuments: List<ReaderDocument> = emptyList(),
    val availableFilters: List<LibraryFilter> = LibraryFilter.defaults(),
    val selectedFilter: LibraryFilter = LibraryFilter.All,
    val badges: List<ReadingBadge> = emptyList(),
    val errorMessage: String? = null
)

sealed interface LibraryEvent {
    data class OpenDocument(val id: Long) : LibraryEvent
    data class Message(@StringRes val messageRes: Int) : LibraryEvent
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val readerRepository: ReaderRepository,
    private val readingStatsRepository: ReadingStatsRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<LibraryFilter>(LibraryFilter.All)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    init {
        combine(
            readerRepository.observeLibrary(),
            readingStatsRepository.observeReadingStats(),
            selectedFilter
        ) { documents, stats, filter ->
            val filteredDocuments = applyFilter(documents, filter)
            LibraryUiState(
                isLoading = false,
                documents = documents,
                filteredDocuments = filteredDocuments,
                availableFilters = LibraryFilter.defaults(),
                selectedFilter = filter,
                badges = buildBadges(stats),
                errorMessage = null
            )
        }
            .onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }

    fun selectFilter(filter: LibraryFilter) {
        selectedFilter.value = filter
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            val result = readerRepository.importDocument(uri)
            result.onSuccess { document ->
                _events.emit(LibraryEvent.OpenDocument(document.id))
            }.onFailure {
                _events.emit(LibraryEvent.Message(R.string.library_import_error))
            }
        }
    }

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            readerRepository.deleteDocument(documentId)
                .onSuccess {
                    _events.emit(LibraryEvent.Message(R.string.library_delete_success))
                }
                .onFailure {
                    _events.emit(LibraryEvent.Message(R.string.library_delete_error))
                }
        }
    }

    private fun applyFilter(
        documents: List<ReaderDocument>,
        filter: LibraryFilter
    ): List<ReaderDocument> = when (filter) {
        LibraryFilter.All -> documents
        LibraryFilter.InProgress -> documents.filter { !it.lastLocation.isNullOrBlank() }
        LibraryFilter.Unread -> documents.filter { it.lastLocation.isNullOrBlank() }
    }

    private fun buildBadges(stats: ReadingStatsSnapshot): List<ReadingBadge> {
        if (stats.streakDays == 0 && stats.booksRead == 0 && stats.pagesRead == 0) return emptyList()
        val badges = mutableListOf<ReadingBadge>()

        if (stats.booksRead >= 1) {
            badges += ReadingBadge(
                "✨",
                R.string.badge_first_book,
                R.string.badge_first_book_supporting
            )
        }
        if (stats.booksRead >= 5) {
            badges += ReadingBadge(
                "📚",
                R.string.badge_five_books,
                R.string.badge_five_books_supporting
            )
        }
        if (stats.pagesRead >= 100) {
            badges += ReadingBadge(
                "📖",
                R.string.badge_100_pages,
                R.string.badge_100_pages_supporting
            )
        }
        if (stats.pagesRead >= 500) {
            badges += ReadingBadge(
                "🏆",
                R.string.badge_500_pages,
                R.string.badge_500_pages_supporting
            )
        }
        if (stats.streakDays >= 3) {
            badges += ReadingBadge("🔥", R.string.badge_streak_3, R.string.badge_streak_3_supporting)
        }
        if (stats.streakDays >= 7) {
            badges += ReadingBadge("🔥", R.string.badge_streak_7, R.string.badge_streak_7_supporting)
        }
        if (stats.streakDays >= 14) {
            badges += ReadingBadge(
                "🔥",
                R.string.badge_streak_14,
                R.string.badge_streak_14_supporting
            )
        }

        return badges
    }
}
