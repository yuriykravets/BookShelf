package com.partitionsoft.bookshelf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteBookEntity::class,
        ReaderDocumentEntity::class,
        ReaderProgressEntity::class,
        ReaderBookmarkEntity::class,
        ReadingSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BooksDatabase : RoomDatabase() {
    abstract fun favoriteBookDao(): FavoriteBookDao

    abstract fun readerDocumentDao(): ReaderDocumentDao

    abstract fun readingSessionDao(): ReadingSessionDao
}
