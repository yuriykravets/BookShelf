package com.partitionsoft.bookshelf.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.partitionsoft.bookshelf.data.subscription.LocalSubscriptionRepository
import com.partitionsoft.bookshelf.data.subscription.LocalSubscriptionBillingRepository
import com.partitionsoft.bookshelf.data.subscription.PlaySubscriptionBillingRepository
import com.partitionsoft.bookshelf.data.subscription.PlayPurchaseLauncher
import com.partitionsoft.bookshelf.data.subscription.local.DataStoreSubscriptionLocalDataSource
import com.partitionsoft.bookshelf.data.subscription.local.SubscriptionLocalDataSource
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionRepository
import com.partitionsoft.bookshelf.domain.subscription.repository.SubscriptionBillingRepository
import com.partitionsoft.bookshelf.domain.subscription.usecase.CancelSubscriptionUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.GetSubscriptionStatusUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.IsFeatureUnlockedUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.IsPremiumUserUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionStatusUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveActiveSubscriptionPlanUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionPlansUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.ObserveSubscriptionBillingAvailabilityUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.RefreshSubscriptionPlansUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.PurchaseSubscriptionUseCase
import com.partitionsoft.bookshelf.domain.subscription.usecase.RestoreSubscriptionUseCase
import com.example.bookshelf.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.Lazy

@Module
@InstallIn(SingletonComponent::class)
object SubscriptionModule {

    @Provides
    @Singleton
    @SubscriptionPreferences
    fun provideSubscriptionDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(SUBSCRIPTION_DATA_STORE_FILE) }
    )

    @Provides
    @Singleton
    fun provideSubscriptionLocalDataSource(
        @SubscriptionPreferences dataStore: DataStore<Preferences>
    ): SubscriptionLocalDataSource = DataStoreSubscriptionLocalDataSource(dataStore)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        localDataSource: SubscriptionLocalDataSource,
        playRepository: Lazy<PlaySubscriptionBillingRepository>
    ): SubscriptionRepository = if (BuildConfig.DEBUG) {
        LocalSubscriptionRepository(localDataSource = localDataSource, allowLocalEntitlement = true)
    } else {
        playRepository.get()
    }

    @Provides
    @Singleton
    fun providePlaySubscriptionBillingRepository(
        @ApplicationContext context: Context
    ): PlaySubscriptionBillingRepository = PlaySubscriptionBillingRepository(
        context = context,
        productId = BuildConfig.PREMIUM_PRODUCT_ID
    )

    @Provides
    fun providePlayPurchaseLauncher(
        repository: PlaySubscriptionBillingRepository
    ): PlayPurchaseLauncher = repository

    @Provides
    @Singleton
    fun provideSubscriptionBillingRepository(
        localDataSource: SubscriptionLocalDataSource,
        playRepository: Lazy<PlaySubscriptionBillingRepository>
    ): SubscriptionBillingRepository = if (BuildConfig.DEBUG) {
        LocalSubscriptionBillingRepository(localDataSource)
    } else {
        playRepository.get()
    }

    @Provides
    fun provideGetSubscriptionStatusUseCase(
        repository: SubscriptionRepository
    ): GetSubscriptionStatusUseCase = GetSubscriptionStatusUseCase(repository)

    @Provides
    fun provideObserveSubscriptionStatusUseCase(
        repository: SubscriptionRepository
    ): ObserveSubscriptionStatusUseCase = ObserveSubscriptionStatusUseCase(repository)

    @Provides
    fun provideIsPremiumUserUseCase(
        repository: SubscriptionRepository
    ): IsPremiumUserUseCase = IsPremiumUserUseCase(repository)

    @Provides
    fun provideIsFeatureUnlockedUseCase(
        repository: SubscriptionRepository
    ): IsFeatureUnlockedUseCase = IsFeatureUnlockedUseCase(repository)

    @Provides fun provideObserveSubscriptionPlansUseCase(repository: SubscriptionBillingRepository) =
        ObserveSubscriptionPlansUseCase(repository)
    @Provides fun provideObserveSubscriptionBillingAvailabilityUseCase(repository: SubscriptionBillingRepository) =
        ObserveSubscriptionBillingAvailabilityUseCase(repository)
    @Provides fun provideRefreshSubscriptionPlansUseCase(repository: SubscriptionBillingRepository) =
        RefreshSubscriptionPlansUseCase(repository)
    @Provides fun provideObserveActiveSubscriptionPlanUseCase(repository: SubscriptionBillingRepository) =
        ObserveActiveSubscriptionPlanUseCase(repository)
    @Provides fun providePurchaseSubscriptionUseCase(repository: SubscriptionBillingRepository) =
        PurchaseSubscriptionUseCase(repository)
    @Provides fun provideRestoreSubscriptionUseCase(repository: SubscriptionBillingRepository) =
        RestoreSubscriptionUseCase(repository)
    @Provides fun provideCancelSubscriptionUseCase(repository: SubscriptionBillingRepository) =
        CancelSubscriptionUseCase(repository)

    private const val SUBSCRIPTION_DATA_STORE_FILE = "subscription.preferences_pb"
}
