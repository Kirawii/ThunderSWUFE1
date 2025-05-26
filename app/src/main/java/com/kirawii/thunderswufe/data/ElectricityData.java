package com.kirawii.thunderswufe.data;

import java.time.LocalDateTime;
import java.util.Objects;

public class ElectricityData {
    private final LocalDateTime timestamp;
    private final double balance;
    private final double change;
    private final String roomNo;
    private final String meterStatus;
    private final String lastUpdateTime;

    public ElectricityData(LocalDateTime timestamp, double balance, double change, String roomNo, String meterStatus, String lastUpdateTime) {
        this.timestamp = timestamp;
        this.balance = balance;
        this.change = change;
        this.roomNo = roomNo;
        this.meterStatus = meterStatus;
        this.lastUpdateTime = lastUpdateTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getBalance() {
        return balance;
    }

    public double getChange() {
        return change;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public String getMeterStatus() {
        return meterStatus;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectricityData that = (ElectricityData) o;
        return Double.compare(that.balance, balance) == 0 &&
                Double.compare(that.change, change) == 0 &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(roomNo, that.roomNo) &&
                Objects.equals(meterStatus, that.meterStatus) &&
                Objects.equals(lastUpdateTime, that.lastUpdateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, balance, change, roomNo, meterStatus, lastUpdateTime);
    }
} 