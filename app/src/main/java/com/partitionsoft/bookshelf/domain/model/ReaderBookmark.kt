package com.partitionsoft.bookshelf.domain.model

data class ReaderBookmark(
    val id: Long,
    val documentId: Long,
    val chapterIndex: Int,
    val scrollY: Int,
    val title: String,
    val createdAtMillis: Long
)
