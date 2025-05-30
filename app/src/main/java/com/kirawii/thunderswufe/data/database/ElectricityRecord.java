package com.kirawii.thunderswufe.data.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.time.LocalDateTime;

@Entity(tableName = "electricity_records")
public class ElectricityRecord {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private LocalDateTime timestamp;
    private double balance;
    private double change;
    private String roomNo;

    public ElectricityRecord() {}

    public ElectricityRecord(long id, LocalDateTime timestamp, double balance, double change, String roomNo) {
        this.id = id;
        this.timestamp = timestamp;
        this.balance = balance;
        this.change = change;
        this.roomNo = roomNo;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
}
