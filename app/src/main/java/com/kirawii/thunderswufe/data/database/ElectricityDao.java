package com.kirawii.thunderswufe.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.time.LocalDateTime;
import java.util.List;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface ElectricityDao {
    @Query("SELECT * FROM electricity_records ORDER BY timestamp DESC")
    Flowable<List<ElectricityRecord>> getAllRecords();
    
    @Query("SELECT * FROM electricity_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    Flowable<List<ElectricityRecord>> getRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecord(ElectricityRecord record);
    
    @Query("SELECT * FROM electricity_records ORDER BY timestamp DESC LIMIT 1")
    ElectricityRecord getLatestRecord();

    @Query("SELECT * FROM electricity_records WHERE roomNo = :roomNo ORDER BY timestamp DESC LIMIT 1")
    ElectricityRecord getLatestRecordByRoom(String roomNo);
} 