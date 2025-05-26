package com.kirawii.thunderswufe.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.Single;

public class UserPreferencesManager {
    private static final String PREFERENCES_NAME = "thunder_preferences";
    private static final String KEY_ROOM_NO = "room_no";
    private static final String KEY_BUILDING_NO = "building_no";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_LOW_BALANCE_THRESHOLD = "low_balance_threshold";

    private final SharedPreferences preferences;

    public UserPreferencesManager(@NonNull Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void saveRoomInfo(String roomNo, String buildingNo) {
        preferences.edit()
                .putString(KEY_ROOM_NO, roomNo)
                .putString(KEY_BUILDING_NO, buildingNo)
                .apply();
    }

    public Single<UserPreferences> getRoomInfoSingle() {
        return Single.fromCallable(() -> new UserPreferences(
                preferences.getString(KEY_ROOM_NO, ""),
                preferences.getString(KEY_BUILDING_NO, ""),
                preferences.getBoolean(KEY_NOTIFICATION_ENABLED, true),
                preferences.getFloat(KEY_LOW_BALANCE_THRESHOLD, 20.0f)
        ));
    }

    public void setNotificationEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
                .apply();
    }

    public void setLowBalanceThreshold(float threshold) {
        preferences.edit()
                .putFloat(KEY_LOW_BALANCE_THRESHOLD, threshold)
                .apply();
    }

    public boolean isNotificationEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public float getLowBalanceThreshold() {
        return preferences.getFloat(KEY_LOW_BALANCE_THRESHOLD, 20.0f);
    }

    public String getRoomNo() {
        return preferences.getString(KEY_ROOM_NO, "");
    }

    public String getBuildingNo() {
        return preferences.getString(KEY_BUILDING_NO, "");
    }
} 