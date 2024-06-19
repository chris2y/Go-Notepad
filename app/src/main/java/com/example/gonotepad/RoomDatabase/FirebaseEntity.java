package com.example.gonotepad.RoomDatabase;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "firebase_locations")
public class FirebaseEntity {
    @PrimaryKey()
    private long id;
    private String location;
    private String companyName;
    private boolean synced;
    private Double longitude;
    private Double latitude;

    public FirebaseEntity() {
    }

    @Ignore
    public FirebaseEntity(long id ,String location, String companyName,boolean synced) {
        this.location = location;
        this.companyName = companyName;
        this.synced = synced;
        this.id = id;
    }
    @Ignore
    public FirebaseEntity(long id, String location, String companyName, boolean synced, Double longitude, Double latitude) {
        this.id = id;
        this.location = location;
        this.companyName = companyName;
        this.synced = synced;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}