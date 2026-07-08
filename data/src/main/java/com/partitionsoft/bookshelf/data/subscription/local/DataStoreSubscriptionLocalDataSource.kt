package com.partitionsoft.bookshelf.data.subscription.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.partitionsoft.bookshelf.domain.subscription.model.SubscriptionStatus
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreSubscriptionLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : SubscriptionLocalDataSource {

    override fun observeStatus(): Flow<SubscriptionStatus> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[SUBSCRIPTION_STATUS_KEY]
                ?.let { storedValue ->
                    SubscriptionStatus.entries.firstOrNull { it.name == storedValue }
                }
                ?: SubscriptionStatus.FREE
        }

    override fun observeActivePlanId(): Flow<String?> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[ACTIVE_PLAN_ID_KEY] }

    override suspend fun updateSubscription(status: SubscriptionStatus, activePlanId: String?) {
        dataStore.edit { preferences ->
            preferences[SUBSCRIPTION_STATUS_KEY] = status.name
            if (activePlanId == null) preferences.remove(ACTIVE_PLAN_ID_KEY)
            else preferences[ACTIVE_PLAN_ID_KEY] = activePlanId
        }
    }

    private companion object {
        val SUBSCRIPTION_STATUS_KEY = stringPreferencesKey("subscription_status")
        val ACTIVE_PLAN_ID_KEY = stringPreferencesKey("active_plan_id")
    }
}
