package com.example.gonotepad;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.gonotepad.R;
import com.example.gonotepad.RoomDatabase.AppDatabase;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AddNoteActivity extends AppCompatActivity {

    private AppDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private double latitude;
    private double longitude;
    private boolean locationFetched = false;

    private long dateId;
    private Boolean newNote;
    private long noteId;
    String[] itemsList = {"1", "2", "3", "4", "5"};
    AutoCompleteTextView autoCompleteTextView;
    ArrayAdapter<String> adapterItems;
    String interestRate;
    TextInputLayout companyName, phoneNumber, email, location, additionalInfo, notes, followUp;

    ImageView back,more;
    TextView backNote;
    private LocationRequest locationRequest;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        companyName = findViewById(R.id.text_input_company);
        phoneNumber = findViewById(R.id.text_input_phone);
        email = findViewById(R.id.text_input_email);
        location = findViewById(R.id.text_input_location);
        additionalInfo = findViewById(R.id.text_input_additional_information);
        notes = findViewById(R.id.text_input_notes);
        followUp = findViewById(R.id.text_input_follow_ups);

        db = AppDatabase.getDatabase(this);


        backNote = findViewById(R.id.backButton2);
        back = findViewById(R.id.backButton1);
        more = findViewById(R.id.moreIcon);

        progressBar = findViewById(R.id.progressBar);

        syncWithFirestore();

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        autoCompleteTextView = findViewById(R.id.autoCompleteText);
        adapterItems = new ArrayAdapter<>(AddNoteActivity.this, android.R.layout.simple_dropdown_item_1line, itemsList);
        autoCompleteTextView.setAdapter(adapterItems);

        autoCompleteTextView.setOnItemClickListener((adapterView, view, i, l) -> {
            interestRate = adapterView.getItemAtPosition(i).toString();
        });

        more.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(AddNoteActivity.this, view);
            popupMenu.inflate(R.menu.popupmenuoption);

            popupMenu.setOnMenuItemClickListener(this::handleMenuItemClick);

            popupMenu.show();
        });

        back.setOnClickListener(view -> saveNoteAndFinish());
        backNote.setOnClickListener(view -> saveNoteAndFinish());

        Intent intent = getIntent();
        if (intent != null) {
            String phonenumber = intent.getStringExtra("phoneNumber");
            String companyname = intent.getStringExtra("companyName");
            String notetext = intent.getStringExtra("noteText");
            String emailText = intent.getStringExtra("email");
            String locationText = intent.getStringExtra("location");
            String additionalInfoText = intent.getStringExtra("additionalInfo");
            String followUpText = intent.getStringExtra("followUp");
            interestRate = intent.getStringExtra("interestRate");
            noteId = intent.getLongExtra("noteId", -1);

            phoneNumber.getEditText().setText(phonenumber);
            companyName.getEditText().setText(companyname);
            notes.getEditText().setText(notetext);
            email.getEditText().setText(emailText);
            location.getEditText().setText(locationText);
            additionalInfo.getEditText().setText(additionalInfoText);
            followUp.getEditText().setText(followUpText);

            if (interestRate != null) {
                autoCompleteTextView.post(() -> autoCompleteTextView.setText(interestRate, false));
            }

            dateId = intent.getLongExtra("dateId", -1);
            newNote = intent.getBooleanExtra("newNote", false);

            if (newNote){
                getCurrentLocation();
            }
        }
    }

    private boolean handleMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.saveAsPdf) {
            // Handle save as PDF option
            checkPermissions();
            return true;
        }
        else if (itemId == R.id.cancel) {
            finish();
            return true;
        }
        return false;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Devices running Android 11 (API 30) or higher
            saveAsPdf();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Devices running Android 6.0 (API 23) to Android 10 (API 29)
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    isGPSEnabled()) {
                // Both permissions are granted, proceed with saving as PDF
                //saveAsPdf();
            } else {
                // Request permissions
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions, 1);
            }
        } else {
            // For devices running below Marshmallow, permissions are granted by default
            //saveAsPdf();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // Permissions granted, proceed with saving as PDF
                //saveAsPdf();
            } else {
                // Permissions denied, show a message or handle it accordingly
                Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveAsPdf() {
        // Gather the data from the fields
        String companyNameStr = companyName.getEditText().getText().toString().trim();
        String locationStr = location.getEditText().getText().toString().trim();
        String phoneNumberStr = phoneNumber.getEditText().getText().toString().trim();
        String emailStr = email.getEditText().getText().toString().trim();
        String interestRateStr = interestRate;
        String additionalInfoStr = additionalInfo.getEditText().getText().toString().trim();
        String notesStr = notes.getEditText().getText().toString().trim();
        String followUpStr = followUp.getEditText().getText().toString().trim();

        // Create a new PdfDocument
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // Initialize a Paint object
        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(android.graphics.Color.BLACK);

        // Define margins for A4 paper size
        int leftMargin = 70;
        int topMargin = 100;
        int rightMargin = 70;
        int bottomMargin = 100;

        // Calculate the usable width
        int usableWidth = pageInfo.getPageWidth() - leftMargin - rightMargin;

        // Line height
        int lineHeight = 25;

        // Start drawing content
        int x = leftMargin;
        int y = topMargin;

        // Draw the title
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(18);
        titlePaint.setColor(android.graphics.Color.BLACK);
        page.getCanvas().drawText("Customer Inquiry Form", x, y, titlePaint);
        y += lineHeight * 2; // Double line height for spacing after title

        // Draw the form fields with proper spacing and wrapping
        y = drawWrappedText(page.getCanvas(), paint, "Company Name: " + companyNameStr, x, y, usableWidth, lineHeight, true);
        y = drawWrappedText(page.getCanvas(), paint, "Location: " + locationStr, x, y, usableWidth, lineHeight, true);
        y = drawWrappedText(page.getCanvas(), paint, "Phone Number: " + phoneNumberStr, x, y, usableWidth, lineHeight, true);
        y = drawWrappedText(page.getCanvas(), paint, "Email: " + emailStr, x, y, usableWidth, lineHeight, true);
        y = drawWrappedText(page.getCanvas(), paint, "Interest Rate: " + interestRateStr, x, y, usableWidth, lineHeight, true);
        y = drawWrappedText(page.getCanvas(), paint, "Additional Information: " + additionalInfoStr, x, y, usableWidth, lineHeight, true);
        y = drawWrappedText(page.getCanvas(), paint, "Notes/Comments: " + notesStr, x, y, usableWidth, lineHeight, true);
        y = drawWrappedText(page.getCanvas(), paint, "Follow-Up Action Required: " + followUpStr, x, y, usableWidth, lineHeight, true);

        // Finish the page
        pdfDocument.finishPage(page);

        // Save the PDF to the Documents directory
        long timestamp = dateId;
        String dateStr = formatDate(timestamp);

        // Create a directory named with the date string
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File dateDir = new File(documentsDir, dateStr);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }

        // Sanitize company name for use as filename (remove invalid characters)
        String sanitizedCompanyName = sanitizeFileName(companyNameStr);

        // Save the PDF to the newly created directory with a sanitized filename
        File pdfFile = new File(dateDir, sanitizedCompanyName + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdfDocument.writeTo(fos);
            Toast.makeText(this, "PDF saved successfully in " + dateDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.d("AddNoteActivity", "PDF saved at: " + pdfFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
            Log.e("AddNoteActivity", "Failed to save PDF: " + e.getMessage()); // Add logging here
        } finally {
            pdfDocument.close();
        }
    }


    private String sanitizeFileName(String fileName) {
        // Replace invalid characters with underscore
        return fileName.replaceAll("[^a-zA-Z0-9]", "_");
    }


    private int drawWrappedText(Canvas canvas, Paint paint, String text, int x, int y, int maxWidth, int lineHeight, boolean addSpacing) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (paint.measureText(line + word) <= maxWidth) {
                line.append(word).append(" ");
            } else {
                canvas.drawText(line.toString(), x, y, paint);
                y += lineHeight;
                line = new StringBuilder(word).append(" ");
            }
        }
        if (!line.toString().isEmpty()) {
            canvas.drawText(line.toString(), x, y, paint);
            y += lineHeight;
        }
        if (addSpacing) {
            y += 15; // Additional space for separation between titles
        }
        return y;
    }

    private String formatDate(long currentTimeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(currentTimeMillis));
    }



    private void saveNoteAndFinish() {
        String companyname = companyName.getEditText().getText().toString().trim();
        String phonenumber = phoneNumber.getEditText().getText().toString().trim();
        String noteText = notes.getEditText().getText().toString().trim();
        String emailText = email.getEditText().getText().toString().trim();
        String locationText = location.getEditText().getText().toString().trim();
        String additionalInfoText = additionalInfo.getEditText().getText().toString().trim();
        String followUpText = followUp.getEditText().getText().toString().trim();

        boolean isValid = true;

        if (companyname.isEmpty()) {
            companyName.setError("Company name is required");
            isValid = false;
        } else {
            companyName.setError(null);
        }

        if (phonenumber.isEmpty()) {
            phoneNumber.setError("Phone number is required");
            isValid = false;
        } else {
            phoneNumber.setError(null);
        }
        if (locationText.isEmpty()) {
            location.setError("Location is required");
            isValid = false;
        } else {
            location.setError(null);
        }

        if (isValid) {
            // If location is not fetched yet, wait and try again
            if (!locationFetched && newNote) {
                getCurrentLocation();
                return; // Exit the method until location is fetched
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("phoneNumber", phonenumber);
            resultIntent.putExtra("companyName", companyname);
            resultIntent.putExtra("noteText", noteText);
            resultIntent.putExtra("email", emailText);
            resultIntent.putExtra("location", locationText);
            resultIntent.putExtra("additionalInfo", additionalInfoText);
            resultIntent.putExtra("followUp", followUpText);
            resultIntent.putExtra("interestRate", interestRate);
            resultIntent.putExtra("dateId", dateId);
            resultIntent.putExtra("noteId", noteId);
            resultIntent.putExtra("latitude", latitude);
            resultIntent.putExtra("longitude", longitude);

            Log.d("AddNoteActivity", "Saving Latitude: " + latitude);
            Log.d("AddNoteActivity", "Saving Longitude: " + longitude);

            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(AddNoteActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
        }
    }



    private void getCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(AddNoteActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    LocationServices.getFusedLocationProviderClient(AddNoteActivity.this)
                            .requestLocationUpdates(locationRequest, new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult locationResult) {
                                    super.onLocationResult(locationResult);
                                    LocationServices.getFusedLocationProviderClient(AddNoteActivity.this)
                                            .removeLocationUpdates(this);
                                    if (locationResult != null && locationResult.getLocations().size() > 0) {
                                        int index = locationResult.getLocations().size() - 1;
                                        latitude = locationResult.getLocations().get(index).getLatitude();
                                        longitude = locationResult.getLocations().get(index).getLongitude();
                                        locationFetched = true;

                                        Log.d("AddNoteActivity", "Fetched Latitude: " + latitude);
                                        Log.d("AddNoteActivity", "Fetched Longitude: " + longitude);
                                    }
                                }
                            }, Looper.getMainLooper());
                } else {
                    turnOnGPS();
                }
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void turnOnGPS() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(AddNoteActivity.this, "GPS is already turned on", Toast.LENGTH_SHORT).show();
                } catch (ApiException e) {
                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(AddNoteActivity.this, 2);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Device does not have location
                            break;
                    }
                }
            }
        });
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;
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


    @Override
    public void onBackPressed() {
        saveNoteAndFinish();
    }

}
