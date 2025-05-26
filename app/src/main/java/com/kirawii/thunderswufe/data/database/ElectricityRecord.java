package com.kirawii.thunderswufe.data.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(tableName = "electricity_records")
public class ElectricityRecord {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private LocalDateTime timestamp;
    private double balance;
    private double change;
    private String roomNo;

    public ElectricityRecord(LocalDateTime timestamp, double balance, double change, String roomNo) {
        this.timestamp = timestamp;
        this.balance = balance;
        this.change = change;
        this.roomNo = roomNo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectricityRecord that = (ElectricityRecord) o;
        return id == that.id &&
                Double.compare(that.balance, balance) == 0 &&
                Double.compare(that.change, change) == 0 &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(roomNo, that.roomNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, balance, change, roomNo);
    }
} 