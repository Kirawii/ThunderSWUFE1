package com.kirawii.thunderswufe.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {ElectricityRecord.class}, version = 3)
@TypeConverters({Converters.class})
public abstract class ElectricityDatabase extends RoomDatabase {
    public abstract ElectricityDao electricityDao();
    public static final String DATABASE_NAME = "electricity_db";
}
