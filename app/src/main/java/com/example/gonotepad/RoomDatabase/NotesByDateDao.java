package com.example.gonotepad.RoomDatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NotesByDateDao {

    @Query("SELECT * FROM notes_by_date WHERE dateId = :dateId")
    List<NotesByDateEntity> getNotesByDateId(long dateId);

    @Insert
    void insert(NotesByDateEntity note);

    @Update
    void update(NotesByDateEntity note);

    @Query("SELECT * FROM notes_by_date WHERE noteId = :noteId")
    NotesByDateEntity getNoteById(long noteId);

}
