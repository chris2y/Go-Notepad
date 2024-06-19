package com.example.gonotepad;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;

public class MapViewActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private final int REQUEST_CHECK_SETTINGS = 2;
    private MapView map = null;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean hasZoomedToUserLocation = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the activity full screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Load/initialize the osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Inflate and create the map
        setContentView(R.layout.activity_map_view);

        map = findViewById(R.id.osmmap);
        map.setTileSource(TileSourceFactory.MAPNIK);

        // Disable map wrapping
        map.setHorizontalMapRepetitionEnabled(false);
        map.setVerticalMapRepetitionEnabled(false);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    zoomToUserLocation(location);
                }
            }
        };

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        });

        fetchAndDisplayLocations();
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            checkGooglePlayServicesAvailability();
        }
    }

    private void checkGooglePlayServicesAvailability() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            // Google Play Services are available
            zoomToUserLocation(null);
        } else {
            // Prompt user to install/update Google Play Services
            GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, 0).show();
        }
    }

    private void zoomToUserLocation(Location lastKnownLocation) {
        if (!hasZoomedToUserLocation) {
            if (lastKnownLocation != null) {
                updateMapWithLocation(lastKnownLocation);
                hasZoomedToUserLocation = true; // Set flag to true after zooming
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        updateMapWithLocation(location);
                                        hasZoomedToUserLocation = true; // Set flag to true after zooming
                                    } else {
                                        // Handle case where location is null, e.g., request a new location update
                                        Log.w("MapViewActivity", "Location is null, requesting new location data");
                                        requestNewLocationData();
                                    }
                                }
                            });
                }
            }
        }
    }


    private void updateMapWithLocation(Location location) {
        GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        map.getController().setZoom(16.0);
        map.getController().setCenter(userLocation);

        // Add a marker for the user's location
        Marker userMarker = new Marker(map);
        userMarker.setPosition(userLocation);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setTitle("You are here");
        userMarker.setIcon(ContextCompat.getDrawable(MapViewActivity.this,
                R.drawable.baseline_location_on_24));
        map.getOverlays().add(userMarker);
    }


    private void requestNewLocationData() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. Request location updates
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapViewActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the osmdroid configuration
        map.onResume(); // Needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save the osmdroid configuration
        map.onPause(); // Needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean permissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = false;
                    break;
                }
            }
            if (permissionsGranted) {
                checkGooglePlayServicesAvailability(); // Retry fetching the location after permissions are granted
            }
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE
            );
        }
    }

    private void fetchAndDisplayLocations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double latitude = document.getDouble("latitude");
                            Double longitude = document.getDouble("longitude");
                            String companyName = document.getString("companyName");
                            String location = document.getString("location");

                            if (latitude != null && longitude != null && companyName != null) {
                                // Skip markers with latitude or longitude equal to 0
                                if (latitude != 0 && longitude != 0) {
                                    // Create and add a marker to the map at the specified location
                                    GeoPoint point = new GeoPoint(latitude, longitude);
                                    Marker marker = new Marker(map);
                                    marker.setPosition(point);
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    marker.setIcon(ContextCompat.getDrawable(MapViewActivity.this,
                                            R.drawable.baseline_account_company_24));
                                    // Set title with company name and coordinates on separate lines
                                    String title = "Company name: " + companyName + "\nLocation: " + location;
                                    marker.setTitle(title);
                                    map.getOverlays().add(marker);
                                }
                            } else {
                                Log.w("MapViewActivity", "Missing data in document: " + document.getId());
                            }
                        }
                        map.invalidate(); // Refresh the map to display the markers
                    } else {
                        Log.e("MapViewActivity", "Error getting documents: ", task.getException());
                    }
                });
    }
}
