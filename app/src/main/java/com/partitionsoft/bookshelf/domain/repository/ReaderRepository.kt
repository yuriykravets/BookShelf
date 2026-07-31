package com.partitionsoft.bookshelf.domain.repository

import android.net.Uri
import com.partitionsoft.bookshelf.domain.model.ReaderBookmark
import com.partitionsoft.bookshelf.domain.model.ReaderAnnotation
import com.partitionsoft.bookshelf.domain.model.ReaderAnnotationType
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import kotlinx.coroutines.flow.Flow

interface ReaderRepository {
    fun observeLibrary(): Flow<List<ReaderDocument>>

    fun observeContinueReading(): Flow<ReaderDocument?>

    fun observeBookmarks(documentId: Long): Flow<List<ReaderBookmark>>
    fun observeAnnotations(documentId: Long): Flow<List<ReaderAnnotation>>

    suspend fun importDocument(uri: Uri): Result<ReaderDocument>

    suspend fun getDocument(documentId: Long): ReaderDocument?

    suspend fun updateProgress(documentId: Long, location: String)

    suspend fun deleteDocument(documentId: Long): Result<Unit>

    suspend fun addBookmark(
        documentId: Long,
        chapterIndex: Int,
        scrollY: Int,
        title: String
    ): Result<Unit>

    suspend fun deleteBookmark(bookmarkId: Long): Result<Unit>

    suspend fun addAnnotation(
        documentId: Long,
        chapterIndex: Int,
        scrollY: Int,
        type: ReaderAnnotationType,
        selectedText: String,
        noteText: String,
        colorHex: String,
        anchor: String? = null
    ): Result<Unit>

    suspend fun deleteAnnotation(annotationId: Long): Result<Unit>
}
