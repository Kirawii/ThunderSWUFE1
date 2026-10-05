package com.kirawii.thunderswufe.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {ElectricityRecord.class}, version = 4, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class ElectricityDatabase extends RoomDatabase {
    public abstract ElectricityDao electricityDao();
    public static final String DATABASE_NAME = "electricity_db";

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP INDEX IF EXISTS index_electricity_records_timestamp_roomNo");
            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_electricity_records_roomNo_timestamp " +
                            "ON electricity_records(roomNo, timestamp)"
            );
        }
    };
}
