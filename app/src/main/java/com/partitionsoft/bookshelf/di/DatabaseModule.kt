package com.partitionsoft.bookshelf.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.partitionsoft.bookshelf.data.local.BooksDatabase
import com.partitionsoft.bookshelf.data.local.FavoriteBookDao
import com.partitionsoft.bookshelf.data.local.ReadingSessionDao
import com.partitionsoft.bookshelf.data.local.ReaderDocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBooksDatabase(@ApplicationContext context: Context): BooksDatabase =
        Room.databaseBuilder(
            context,
            BooksDatabase::class.java,
            "bookshelf.db"
        )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideFavoriteBookDao(database: BooksDatabase): FavoriteBookDao =
        database.favoriteBookDao()

    @Provides
    @Singleton
    fun provideReaderDocumentDao(database: BooksDatabase): ReaderDocumentDao =
        database.readerDocumentDao()

    @Provides
    @Singleton
    fun provideReadingSessionDao(database: BooksDatabase): ReadingSessionDao =
        database.readingSessionDao()

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reader_annotations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `documentId` INTEGER NOT NULL,
                    `chapterIndex` INTEGER NOT NULL,
                    `scrollY` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `selectedText` TEXT NOT NULL,
                    `noteText` TEXT NOT NULL,
                    `colorHex` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_reader_annotations_documentId_chapterIndex_scrollY` " +
                    "ON `reader_annotations` (`documentId`, `chapterIndex`, `scrollY`)"
            )
        }
    }
}
