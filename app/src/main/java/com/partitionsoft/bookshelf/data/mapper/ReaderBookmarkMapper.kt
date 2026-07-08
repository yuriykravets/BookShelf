package com.partitionsoft.bookshelf.data.mapper

import com.partitionsoft.bookshelf.data.local.ReaderBookmarkEntity
import com.partitionsoft.bookshelf.domain.model.ReaderBookmark

fun ReaderBookmarkEntity.toDomain(): ReaderBookmark = ReaderBookmark(
    id = id,
    documentId = documentId,
    chapterIndex = chapterIndex,
    scrollY = scrollY,
    title = title,
    createdAtMillis = createdAtMillis
)
