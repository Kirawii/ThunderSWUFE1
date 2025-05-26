package com.kirawii.thunderswufe.data.model;

import java.time.LocalDateTime;

public class ElectricityData {
    private final LocalDateTime timestamp;
    private final double balance;
    private final double change;
    private final String roomNo;
    private final String meterStatus;

    public ElectricityData(LocalDateTime timestamp, double balance, double change, String roomNo, String meterStatus) {
        this.timestamp = timestamp;
        this.balance = balance;
        this.change = change;
        this.roomNo = roomNo;
        this.meterStatus = meterStatus;
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
} 