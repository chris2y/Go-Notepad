package com.example.gonotepad;



import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gonotepad.Adapters.DateAdapter;
import com.example.gonotepad.RoomDatabase.AppDatabase;
import com.example.gonotepad.RoomDatabase.DateDao;
import com.example.gonotepad.RoomDatabase.DateEntity;
import com.example.gonotepad.RoomDatabase.FirebaseDao;
import com.example.gonotepad.RoomDatabase.FirebaseEntity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.Nullable;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class MainActivity extends AppCompatActivity {

    private AppDatabase db;

    private DateDao dateDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DateAdapter dateAdapter;


    private ImageView locationView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(this);
        dateDao = db.dateDao();

        locationView = findViewById(R.id.locationView);

        syncWithFirestore();

        locationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapViewActivity.class);
                startActivity(intent);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerViewDates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dateAdapter = new DateAdapter(MainActivity.this);
        recyclerView.setAdapter(dateAdapter);

        FloatingActionButton buttonSaveDate = findViewById(R.id.button_save_date);


        buttonSaveDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentTimeMillis();
            }
        });

        loadSavedDates();



    }

    private void saveCurrentTimeMillis() {
        long currentTimeMillis = System.currentTimeMillis();
        DateEntity dateEntity = new DateEntity(currentTimeMillis);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // Check if a date for the current day is already saved
                boolean isDateSavedForToday = isDateSavedForToday();
                if (isDateSavedForToday) {
                    // If a date for today is already saved, show a toast message
                    showToast("Date already added");
                    //dateDao.insert(dateEntity);
                } else {
                    // If no date for today is saved, insert the new date into the database
                    dateDao.insert(dateEntity);
                }

                // Refresh RecyclerView after insertion
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadSavedDates();
                    }
                });
            }
        });
    }

    private boolean isDateSavedForToday() {
        long startOfDay = getStartOfDay(System.currentTimeMillis());
        long endOfDay = getEndOfDay(System.currentTimeMillis());
        List<DateEntity> dates = dateDao.getDatesBetween(startOfDay, endOfDay);
        return !dates.isEmpty();
    }

    private long getStartOfDay(long currentTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay(long currentTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }



    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadSavedDates() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<DateEntity> dateEntities = dateDao.getAllDates();

                // Convert DateEntities to formatted date strings
                List<Long> dateStrings = new ArrayList<>();
                for (DateEntity entity : dateEntities) {
                    dateStrings.add(entity.currentTimeMillis);
                }

                // Reverse the list
                Collections.reverse(dateStrings);

                // Update RecyclerView on the main thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dateAdapter.setDates(dateStrings);
                    }
                });
            }
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
