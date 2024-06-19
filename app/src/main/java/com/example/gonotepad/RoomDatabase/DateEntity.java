package com.example.gonotepad.RoomDatabase;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DateEntity {

        @PrimaryKey(autoGenerate = true)
        public int id;
        public long currentTimeMillis;

        public DateEntity(long currentTimeMillis) {
                this.currentTimeMillis = currentTimeMillis;
        }
}
