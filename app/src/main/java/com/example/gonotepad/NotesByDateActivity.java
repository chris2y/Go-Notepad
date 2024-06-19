package com.example.gonotepad;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gonotepad.Adapters.NotesByDateAdapter;
import com.example.gonotepad.RoomDatabase.AppDatabase;
import com.example.gonotepad.RoomDatabase.FirebaseDao;
import com.example.gonotepad.RoomDatabase.FirebaseEntity;
import com.example.gonotepad.RoomDatabase.NotesByDateEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesByDateActivity extends AppCompatActivity {

    private static final int ADD_NOTE_REQUEST_CODE = 1;
    private static final int EDIT_NOTE_REQUEST_CODE = 2;
    private AppDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private NotesByDateAdapter notesByDateAdapter;
    private long dateId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_by_date);

        db = AppDatabase.getDatabase(this);
        dateId = getIntent().getLongExtra("itemId", -1);

        FloatingActionButton buttonAddNote = findViewById(R.id.button_add_note);
        RecyclerView recyclerViewNotesByDate = findViewById(R.id.recyclerViewNotesByDate);

        notesByDateAdapter = new NotesByDateAdapter(this);
        recyclerViewNotesByDate.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotesByDate.setAdapter(notesByDateAdapter);

        syncWithFirestore();

        buttonAddNote.setOnClickListener(view -> {
            Intent intent = new Intent(NotesByDateActivity.this, AddNoteActivity.class);
            intent.putExtra("dateId", dateId);
            intent.putExtra("newNote", true);
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE);
        });

        loadNotesByDate(dateId);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String phoneNumber = data.getStringExtra("phoneNumber");
            String companyName = data.getStringExtra("companyName");
            String noteText = data.getStringExtra("noteText");
            String email = data.getStringExtra("email");
            String location = data.getStringExtra("location");
            String additionalInfo = data.getStringExtra("additionalInfo");
            String followUp = data.getStringExtra("followUp");
            String interestRate = data.getStringExtra("interestRate");
            double latitude = data.getDoubleExtra("latitude", 0.0);
            double longitude = data.getDoubleExtra("longitude", 0.0);


            Log.d("NotesByDateActivity", "Received Latitude: " + latitude);
            Log.d("NotesByDateActivity", "Received Longitude: " + longitude);

            long noteId = data.getLongExtra("noteId", -1);

            if (requestCode == ADD_NOTE_REQUEST_CODE) {
                NotesByDateEntity note = new NotesByDateEntity(System.currentTimeMillis(), dateId, noteText, phoneNumber,
                        companyName, email, location, additionalInfo, followUp, interestRate);
                FirebaseEntity firebaseSave = new FirebaseEntity(note.getNoteId(), location, companyName, false, longitude, latitude);
                addNoteToDatabase(note, firebaseSave);
                Toast.makeText(this, "Note Added", Toast.LENGTH_SHORT).show();
            } else if (requestCode == EDIT_NOTE_REQUEST_CODE) {
                NotesByDateEntity note = new NotesByDateEntity(noteId, dateId, noteText, phoneNumber,
                        companyName, email, location, additionalInfo, followUp, interestRate);
                FirebaseEntity firebaseSave = new FirebaseEntity(noteId, location, companyName, false, longitude, latitude);
                updateNoteInDatabase(note, firebaseSave);
            }
        }
    }


    private void loadNotesByDate(long dateId) {
        executorService.execute(() -> {
            List<NotesByDateEntity> notesByDateEntities = db.notesByDateDao().getNotesByDateId(dateId);
            Log.d("NotesByDateActivity", "Fetched " + notesByDateEntities.size() + " notes from database.");
            runOnUiThread(() -> {
                Collections.reverse(notesByDateEntities);
                notesByDateAdapter.setNotesByDate(notesByDateEntities);
            });
        });
    }

    private void addNoteToDatabase(NotesByDateEntity note, FirebaseEntity firebaseSave) {
        executorService.execute(() -> {
            Log.d("NotesByDateActivity", "Adding note: " + note.getNoteText());
            Log.d("NotesByDateActivity", "Saving Latitude: " + firebaseSave.getLatitude());
            Log.d("NotesByDateActivity", "Saving Longitude: " + firebaseSave.getLongitude());
            db.notesByDateDao().insert(note);
            firebaseSave.setId(note.getNoteId());  // Ensure FirebaseEntity ID matches NotesByDateEntity ID
            db.firebaseDao().insert(firebaseSave);
            loadNotesByDate(dateId);
            displayUnsyncedCount();
        });
    }

    private void updateNoteInDatabase(NotesByDateEntity note, FirebaseEntity firebaseSave) {
        executorService.execute(() -> {
            Log.d("NotesByDateActivity", "Updating note with ID: " + note.getNoteId() + ", Note Text: " + note.getNoteText());
            Log.d("NotesByDateActivity", "Saving Latitude: " + firebaseSave.getLatitude());
            Log.d("NotesByDateActivity", "Saving Longitude: " + firebaseSave.getLongitude());
            db.notesByDateDao().update(note);
            db.firebaseDao().update(firebaseSave);

            // Log updated values
            NotesByDateEntity updatedNote = db.notesByDateDao().getNoteById(note.getNoteId());
            FirebaseEntity updatedFirebaseEntity = db.firebaseDao().getFirebaseEntityById(firebaseSave.getId());

            Log.d("NotesByDateActivity", "Updated note: " + updatedNote.getNoteText());
            Log.d("NotesByDateActivity", "Updated FirebaseEntity: Company Name - " + updatedFirebaseEntity.getCompanyName() +
                    ", Synced - " + updatedFirebaseEntity.isSynced() + ", Latitude - " + updatedFirebaseEntity.getLatitude() +
                    ", Longitude - " + updatedFirebaseEntity.getLongitude());

            loadNotesByDate(dateId);
            displayUnsyncedCount();
        });
    }

    private void displayUnsyncedCount() {
        executorService.execute(() -> {
            FirebaseDao firebaseDao = db.firebaseDao();
            List<FirebaseEntity> unsyncedEntities = firebaseDao.getUnsynced();
            int unsyncedCount = unsyncedEntities.size();

            String toastMessage = "You have " + unsyncedCount + " unsynchronized locations.";
            Log.d("NotesByDateActivity", toastMessage);
        });
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void syncWithFirestore() {
        if (isOnline()) {
            executorService.execute(() -> {
                FirebaseDao firebaseDao = db.firebaseDao();
                List<FirebaseEntity> unsyncedEntities = firebaseDao.getUnsynced();

                for (FirebaseEntity entity : unsyncedEntities) {
                    Map<String, Object> locationData = new HashMap<>();
                    locationData.put("location", entity.getLocation());
                    locationData.put("companyName", entity.getCompanyName());
                    locationData.put("longitude", entity.getLongitude());
                    locationData.put("latitude", entity.getLatitude());

                    String docId = String.valueOf(entity.getId());

                    FirebaseFirestore.getInstance().collection("locations")
                            .document(docId)
                            .set(locationData)
                            .addOnSuccessListener(aVoid -> {
                                entity.setSynced(true);
                                executorService.execute(() -> {
                                    firebaseDao.update(entity);
                                    Log.d("NotesByDateActivity", "Synced entity with ID: " + entity.getId());
                                });
                            })
                            .addOnFailureListener(e -> Log.e("NotesByDateActivity", "Error syncing data with Firestore", e));
                }
            });
        }
    }
}
