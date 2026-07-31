package com.partitionsoft.bookshelf.data.repository

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.partitionsoft.bookshelf.data.local.ReaderBookmarkEntity
import com.partitionsoft.bookshelf.data.local.ReaderAnnotationEntity
import com.partitionsoft.bookshelf.data.local.ReaderDocumentDao
import com.partitionsoft.bookshelf.data.local.ReaderDocumentEntity
import com.partitionsoft.bookshelf.data.local.ReaderProgressEntity
import com.partitionsoft.bookshelf.data.mapper.toDomain
import com.partitionsoft.bookshelf.data.reader.ReaderFileFormatDetector
import com.partitionsoft.bookshelf.domain.model.ReaderBookmark
import com.partitionsoft.bookshelf.domain.model.ReaderAnnotation
import com.partitionsoft.bookshelf.domain.model.ReaderAnnotationType
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import com.partitionsoft.bookshelf.domain.model.ReaderDocumentFormat
import com.partitionsoft.bookshelf.domain.repository.ReaderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val readerDocumentDao: ReaderDocumentDao
) : ReaderRepository {

    override fun observeLibrary(): Flow<List<ReaderDocument>> =
        readerDocumentDao.observeLibrary().map { rows -> rows.map { it.toDomain() } }

    override fun observeContinueReading(): Flow<ReaderDocument?> =
        readerDocumentDao.observeContinueReading().map { row -> row?.toDomain() }

    override fun observeBookmarks(documentId: Long): Flow<List<ReaderBookmark>> =
        readerDocumentDao.observeBookmarks(documentId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAnnotations(documentId: Long): Flow<List<ReaderAnnotation>> =
        readerDocumentDao.observeAnnotations(documentId).map { rows ->
            rows.map { row ->
                ReaderAnnotation(
                    id = row.id,
                    documentId = row.documentId,
                    chapterIndex = row.chapterIndex,
                    scrollY = row.scrollY,
                    type = ReaderAnnotationType.valueOf(row.type),
                    selectedText = row.selectedText,
                    noteText = row.noteText,
                    colorHex = row.colorHex,
                    anchor = row.anchor,
                    createdAtMillis = row.createdAtMillis
                )
            }
        }

    override suspend fun importDocument(uri: Uri): Result<ReaderDocument> = runCatching {
        val mimeType = contentResolver.getType(uri)
        val title = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment ?: "Imported book"

        val format = ReaderFileFormatDetector.detect(uri, mimeType, displayName = title)
        require(format != ReaderDocumentFormat.UNKNOWN) { "Unsupported format" }

        val existing = readerDocumentDao.getDocumentByUri(uri.toString())
        if (existing != null) {
            val progress = readerDocumentDao.getProgressLocation(existing.id)
            return@runCatching existing.toDomain(progress)
        }

        val insertedId = readerDocumentDao.insertDocument(
            ReaderDocumentEntity(
                title = title,
                uri = uri.toString(),
                format = format.name,
                mimeType = mimeType,
                addedAtMillis = System.currentTimeMillis()
            )
        )

        val document = if (insertedId > 0) {
            readerDocumentDao.getDocumentById(insertedId)
        } else {
            readerDocumentDao.getDocumentByUri(uri.toString())
        } ?: error("Unable to import document")

        document.toDomain(lastLocation = null)
    }

    override suspend fun getDocument(documentId: Long): ReaderDocument? {
        val entity = readerDocumentDao.getDocumentById(documentId) ?: return null
        val progress = readerDocumentDao.getProgressLocation(documentId)
        return entity.toDomain(progress)
    }

    override suspend fun updateProgress(documentId: Long, location: String) {
        readerDocumentDao.upsertProgress(
            ReaderProgressEntity(
                documentId = documentId,
                location = location,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteDocument(documentId: Long): Result<Unit> = runCatching {
        val document = readerDocumentDao.getDocumentById(documentId) ?: return@runCatching
        runCatching {
            contentResolver.releasePersistableUriPermission(
                document.uri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        readerDocumentDao.deleteDocument(documentId)
    }

    override suspend fun addBookmark(
        documentId: Long,
        chapterIndex: Int,
        scrollY: Int,
        title: String
    ): Result<Unit> = runCatching {
        readerDocumentDao.insertBookmark(
            ReaderBookmarkEntity(
                documentId = documentId,
                chapterIndex = chapterIndex.coerceAtLeast(0),
                scrollY = scrollY.coerceAtLeast(0),
                title = title.ifBlank { "Bookmark" },
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteBookmark(bookmarkId: Long): Result<Unit> = runCatching {
        readerDocumentDao.deleteBookmark(bookmarkId)
    }

    override suspend fun addAnnotation(
        documentId: Long,
        chapterIndex: Int,
        scrollY: Int,
        type: ReaderAnnotationType,
        selectedText: String,
        noteText: String,
        colorHex: String,
        anchor: String?
    ): Result<Unit> = runCatching {
        readerDocumentDao.insertAnnotation(
            ReaderAnnotationEntity(
                documentId = documentId,
                chapterIndex = chapterIndex.coerceAtLeast(0),
                scrollY = scrollY.coerceAtLeast(0),
                type = type.name,
                selectedText = selectedText.trim(),
                noteText = noteText.trim(),
                colorHex = colorHex,
                anchor = anchor,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteAnnotation(annotationId: Long): Result<Unit> = runCatching {
        readerDocumentDao.deleteAnnotation(annotationId)
    }
}
