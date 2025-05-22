package com.kirawii.thunderswufe.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesManager(private val context: Context) {
    
    companion object {
        private val LOW_BALANCE_THRESHOLD = doublePreferencesKey("low_balance_threshold")
        private val ROOM_NO = stringPreferencesKey("room_no")
        private val BUILDING_NO = stringPreferencesKey("building_no")
        private val AREA_NO = stringPreferencesKey("area_no")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
    }
    
    val lowBalanceThreshold: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[LOW_BALANCE_THRESHOLD] ?: 10.0
        }
        
    val roomInfo: Flow<RoomInfo> = context.dataStore.data
        .map { preferences ->
            RoomInfo(
                roomNo = preferences[ROOM_NO] ?: "",
                buildingNo = preferences[BUILDING_NO] ?: "",
                areaNo = preferences[AREA_NO] ?: ""
            )
        }
        
    val authToken: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[AUTH_TOKEN] ?: ""
        }
        
    val notificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_ENABLED] ?: true
        }
    
    suspend fun updateLowBalanceThreshold(threshold: Double) {
        context.dataStore.edit { preferences ->
            preferences[LOW_BALANCE_THRESHOLD] = threshold
        }
    }
    
    suspend fun updateRoomInfo(roomInfo: RoomInfo) {
        context.dataStore.edit { preferences ->
            preferences[ROOM_NO] = roomInfo.roomNo
            preferences[BUILDING_NO] = roomInfo.buildingNo
            preferences[AREA_NO] = roomInfo.areaNo
        }
    }
    
    suspend fun updateAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }
    
    suspend fun updateNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun initializeDefaultsIfNeeded() {
        val currentPrefs = context.dataStore.data.first()
        val roomNoIsSet = currentPrefs[ROOM_NO]?.isNotEmpty() ?: false
        val authTokenIsSet = currentPrefs[AUTH_TOKEN]?.isNotEmpty() ?: false

        if (!roomNoIsSet) {
            val defaultRoomInfo = RoomInfo(
                areaNo = "信园",
                buildingNo = "信园B座",
                roomNo = "XYYB-604"
            )
            updateRoomInfo(defaultRoomInfo)
            // Optionally, log that default room info was set
            // Log.i("UserPreferencesManager", "Default room info set.")
        }

        if (!authTokenIsSet) {
            // val defaultAuthToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzYwMjQxNTMxLCJpYXQiOjE3Mjg3MDU1MzEsImp0aSI6IjM3NzA0YjVjLTQyOTEtNGVlZC05ZThkLTRmOWVkMTM5YTRhYyJ9.Y5XOG2n2wH4pXKlgg1HxPhOXSdr7dNC4ZX0fnPPPeNg" // Old Token
            val defaultAuthToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzc5MTAyOTA0LCJpYXQiOjE3NDc1NjY5MDQsImp0aSI6IjAwZmM3NDFjLTQ1ZmUtNDk0OC1iOWY1LTIyOGM3YzJlMmZlYiJ9.tpk_DTtgWDZogMse-rQWsfdK1GxV92ao-r_qFmHUjYo" // + New Token from successful log
            updateAuthToken(defaultAuthToken)
            // Optionally, log that default auth token was set
            // Log.i("UserPreferencesManager", "Default auth token set.")
        }
    }
}

data class RoomInfo(
    val roomNo: String,
    val buildingNo: String,
    val areaNo: String
) 