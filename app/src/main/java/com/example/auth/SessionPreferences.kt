package com.example.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mbgPrefs by preferencesDataStore(name = "mbg_stock_session")

class SessionPreferences(private val context: Context) {
    private val keyOperatorId = stringPreferencesKey("active_operator_id")
    private val keyStoreId = stringPreferencesKey("active_store_id")

    val savedSession: Flow<Pair<String?, String?>> = context.mbgPrefs.data.map { prefs ->
        prefs[keyStoreId] to prefs[keyOperatorId]
    }

    suspend fun saveActiveOperator(storeId: String, operatorId: String) {
        context.mbgPrefs.edit { prefs ->
            prefs[keyStoreId] = storeId
            prefs[keyOperatorId] = operatorId
        }
    }

    suspend fun clearActiveOperator() {
        context.mbgPrefs.edit { prefs ->
            prefs.remove(keyOperatorId)
        }
    }

    suspend fun clearAll() {
        context.mbgPrefs.edit { it.clear() }
    }
}
