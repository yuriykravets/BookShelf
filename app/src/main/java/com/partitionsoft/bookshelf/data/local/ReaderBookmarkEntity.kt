package com.partitionsoft.bookshelf.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reader_bookmarks",
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["documentId", "chapterIndex", "scrollY"])
    ]
)
data class ReaderBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val chapterIndex: Int,
    val scrollY: Int,
    val title: String,
    val createdAtMillis: Long
)
