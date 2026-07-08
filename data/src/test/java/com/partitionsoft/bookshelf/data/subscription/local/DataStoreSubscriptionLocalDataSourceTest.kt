package com.partitionsoft.bookshelf.data.subscription.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSubscriptionLocalDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `missing preference defaults to free and updates persist`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { temporaryFolder.newFile("subscription.preferences_pb") }
        )
        val dataSource = DataStoreSubscriptionLocalDataSource(dataStore)

        assertEquals(SubscriptionStatus.FREE, dataSource.observeStatus().first())

        dataSource.updateSubscription(SubscriptionStatus.PREMIUM, "yearly")

        assertEquals(SubscriptionStatus.PREMIUM, dataSource.observeStatus().first())
        assertEquals("yearly", dataSource.observeActivePlanId().first())
    }
}
