package com.partitionsoft.bookshelf.domain.repository

import android.net.Uri
import com.partitionsoft.bookshelf.domain.model.ReaderDocument
import kotlinx.coroutines.flow.Flow

interface ReaderRepository {
    fun observeLibrary(): Flow<List<ReaderDocument>>

    fun observeContinueReading(): Flow<ReaderDocument?>

    suspend fun importDocument(uri: Uri): Result<ReaderDocument>

    suspend fun getDocument(documentId: Long): ReaderDocument?

    suspend fun updateProgress(documentId: Long, location: String)

    suspend fun deleteDocument(documentId: Long): Result<Unit>
}
