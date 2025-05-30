package com.kirawii.thunderswufe.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferencesManager {
    private static final String PREF_NAME = "settings";
    private static final String KEY_LOW_BALANCE_THRESHOLD = "low_balance_threshold";
    private static final String KEY_ROOM_NO = "room_no";
    private static final String KEY_BUILDING_NO = "building_no";
    private static final String KEY_AREA_NO = "area_no";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";

    private final SharedPreferences prefs;

    public UserPreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public double getLowBalanceThreshold() {
        return Double.longBitsToDouble(prefs.getLong(KEY_LOW_BALANCE_THRESHOLD, Double.doubleToRawLongBits(10.0)));
    }

    public void setLowBalanceThreshold(double threshold) {
        prefs.edit().putLong(KEY_LOW_BALANCE_THRESHOLD, Double.doubleToRawLongBits(threshold)).apply();
    }

    public RoomInfo getRoomInfo() {
        return new RoomInfo(
                prefs.getString(KEY_ROOM_NO, ""),
                prefs.getString(KEY_BUILDING_NO, ""),
                prefs.getString(KEY_AREA_NO, "")
        );
    }

    public void setRoomInfo(RoomInfo info) {
        prefs.edit()
                .putString(KEY_ROOM_NO, info.getRoomNo())
                .putString(KEY_BUILDING_NO, info.getBuildingNo())
                .putString(KEY_AREA_NO, info.getAreaNo())
                .apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "");
    }

    public void setAuthToken(String token) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    public boolean isNotificationEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }

    public void initializeDefaultsIfNeeded() {
        if (prefs.getString(KEY_ROOM_NO, null) == null) {
            setRoomInfo(new RoomInfo("XYYB-604", "信园B座", "信园"));
        }
        if (prefs.getString(KEY_AUTH_TOKEN, null) == null) {
            setAuthToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzc5MTAyOTA0LCJpYXQiOjE3NDc1NjY5MDQsImp0aSI6IjAwZmM3NDFjLTQ1ZmUtNDk0OC1iOWY1LTIyOGM3YzJlMmZlYiJ9.tpk_DTtgWDZogMse-rQWsfdK1GxV92ao-r_qFmHUjYo");
        }
    }
}
