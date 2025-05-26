package com.kirawii.thunderswufe.data.database;

import androidx.room.TypeConverter;
import java.time.LocalDateTime;

public class Converters {
    @TypeConverter
    public LocalDateTime fromTimestamp(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }

    @TypeConverter
    public String dateToTimestamp(LocalDateTime date) {
        return date == null ? null : date.toString();
    }
} 