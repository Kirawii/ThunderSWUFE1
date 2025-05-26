package com.kirawii.thunderswufe.data;

import java.util.Objects;

public class UserPreferences {
    private final String roomNo;
    private final String buildingNo;
    private final String areaNo;
    private final String authToken;

    public UserPreferences(String roomNo, String buildingNo, String areaNo, String authToken) {
        this.roomNo = roomNo;
        this.buildingNo = buildingNo;
        this.areaNo = areaNo;
        this.authToken = authToken;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public String getAreaNo() {
        return areaNo;
    }

    public String getAuthToken() {
        return authToken;
    }

    public UserPreferences withAuthToken(String newAuthToken) {
        return new UserPreferences(roomNo, buildingNo, areaNo, newAuthToken);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreferences that = (UserPreferences) o;
        return Objects.equals(roomNo, that.roomNo) &&
                Objects.equals(buildingNo, that.buildingNo) &&
                Objects.equals(areaNo, that.areaNo) &&
                Objects.equals(authToken, that.authToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNo, buildingNo, areaNo, authToken);
    }
} 