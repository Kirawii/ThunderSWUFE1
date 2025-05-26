package com.kirawii.thunderswufe.data.preferences;

public class UserPreferences {
    private final String roomNo;
    private final String buildingNo;
    private final boolean notificationEnabled;
    private final float lowBalanceThreshold;

    public UserPreferences(String roomNo, String buildingNo, boolean notificationEnabled, float lowBalanceThreshold) {
        this.roomNo = roomNo;
        this.buildingNo = buildingNo;
        this.notificationEnabled = notificationEnabled;
        this.lowBalanceThreshold = lowBalanceThreshold;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public float getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }
} 