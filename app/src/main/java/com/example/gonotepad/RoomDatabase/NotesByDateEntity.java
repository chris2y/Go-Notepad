package com.example.gonotepad.RoomDatabase;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes_by_date")
public class NotesByDateEntity {
    @PrimaryKey()
    private long noteId;
    private long dateId;
    private String noteText;
    private String phoneNumber;
    private String companyName;
    private String email;
    private String location;
    private String additionalInfo;
    private String followUp;
    private String interestRate;

    public NotesByDateEntity(long noteId, long dateId, String noteText, String phoneNumber, String companyName, String email, String location, String additionalInfo, String followUp, String interestRate) {
        this.noteId = noteId;
        this.dateId = dateId;
        this.noteText = noteText;
        this.phoneNumber = phoneNumber;
        this.companyName = companyName;
        this.email = email;
        this.location = location;
        this.additionalInfo = additionalInfo;
        this.followUp = followUp;
        this.interestRate = interestRate;
    }

    public long getNoteId() {
        return noteId;
    }

    public void setNoteId(long noteId) {
        this.noteId = noteId;
    }

    public long getDateId() {
        return dateId;
    }

    public void setDateId(long dateId) {
        this.dateId = dateId;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getFollowUp() {
        return followUp;
    }

    public void setFollowUp(String followUp) {
        this.followUp = followUp;
    }

    public String getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(String interestRate) {
        this.interestRate = interestRate;
    }
}
