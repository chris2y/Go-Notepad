package com.example.gonotepad.RoomDatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DateDao {
    @Insert
    void insert(DateEntity dateEntity);

    @Query("SELECT * FROM DateEntity")
    List<DateEntity> getAllDates();

    @Query("SELECT * FROM DateEntity WHERE currentTimeMillis = :millis LIMIT 1")
    DateEntity getDateByMillis(long millis);

    @Query("SELECT * FROM DateEntity WHERE currentTimeMillis BETWEEN :startTime AND :endTime")
    List<DateEntity> getDatesBetween(long startTime, long endTime);
}
