package com.example.gonotepad.RoomDatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FirebaseDao {
    @Insert()
    void insert(FirebaseEntity firebaseEntity);

    @Update
    void update(FirebaseEntity firebaseEntity);

    @Query("SELECT * FROM firebase_locations WHERE synced = 0")
    List<FirebaseEntity> getUnsynced();

    @Query("SELECT * FROM firebase_locations WHERE id = :id")
    FirebaseEntity getFirebaseEntityById(long id);

}