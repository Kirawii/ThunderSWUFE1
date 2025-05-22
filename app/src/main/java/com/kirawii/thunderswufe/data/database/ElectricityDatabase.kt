package com.kirawii.thunderswufe.data.database

import androidx.room.*
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "electricity_records")
data class ElectricityRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: LocalDateTime,
    val balance: Double,
    val change: Double,
    val roomNo: String
)

@Dao
interface ElectricityDao {
    @Query("SELECT * FROM electricity_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ElectricityRecord>>
    
    @Query("SELECT * FROM electricity_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getRecordsByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<ElectricityRecord>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ElectricityRecord)
    
    @Query("SELECT * FROM electricity_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): ElectricityRecord?

    @Query("SELECT * FROM electricity_records WHERE roomNo = :roomNo ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecordByRoom(roomNo: String): ElectricityRecord?
}

@Database(entities = [ElectricityRecord::class], version = 1)
@TypeConverters(Converters::class)
abstract class ElectricityDatabase : RoomDatabase() {
    abstract fun electricityDao(): ElectricityDao
    
    companion object {
        const val DATABASE_NAME = "electricity_db"
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.toString()
    }
} 