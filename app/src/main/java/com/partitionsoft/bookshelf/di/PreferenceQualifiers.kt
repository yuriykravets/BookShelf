package com.partitionsoft.bookshelf.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SubscriptionPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReaderPreferences
