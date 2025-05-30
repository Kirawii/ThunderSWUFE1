package com.kirawii.thunderswufe.data;

import java.time.LocalDateTime;
import java.util.Objects;

public class ElectricityData {
    private LocalDateTime timestamp;
    private double balance;
    private double change;
    private String roomNo;
    private String meterStatus;

    public ElectricityData() {}

    public ElectricityData(LocalDateTime timestamp, double balance, double change, String roomNo, String meterStatus) {
        this.timestamp = timestamp;
        this.balance = balance;
        this.change = change;
        this.roomNo = roomNo;
        this.meterStatus = meterStatus;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getMeterStatus() { return meterStatus; }
    public void setMeterStatus(String meterStatus) { this.meterStatus = meterStatus; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectricityData that = (ElectricityData) o;
        return Double.compare(that.balance, balance) == 0 && Double.compare(that.change, change) == 0 && Objects.equals(timestamp, that.timestamp) && Objects.equals(roomNo, that.roomNo) && Objects.equals(meterStatus, that.meterStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, balance, change, roomNo, meterStatus);
    }
}

class UserPreferences {
    private String roomNo;
    private String buildingNo;
    private String areaNo;
    private String authToken;

    public UserPreferences() {}

    public UserPreferences(String roomNo, String buildingNo, String areaNo, String authToken) {
        this.roomNo = roomNo;
        this.buildingNo = buildingNo;
        this.areaNo = areaNo;
        this.authToken = authToken;
    }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getBuildingNo() { return buildingNo; }
    public void setBuildingNo(String buildingNo) { this.buildingNo = buildingNo; }
    public String getAreaNo() { return areaNo; }
    public void setAreaNo(String areaNo) { this.areaNo = areaNo; }
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreferences that = (UserPreferences) o;
        return Objects.equals(roomNo, that.roomNo) && Objects.equals(buildingNo, that.buildingNo) && Objects.equals(areaNo, that.areaNo) && Objects.equals(authToken, that.authToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNo, buildingNo, areaNo, authToken);
    }
} 