package com.example.stay_healthy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class z_MapTrackActivity extends AppCompatActivity {
    private GoogleMap mMap;
    private FusedLocationProviderClient fused;
    private LocationCallback callback;
    private List<LatLng> path = new ArrayList<>();
    private Button startBtn, stopBtn;
    private TextView statusText, distanceText, timeText, caloriesText; // New data fields
    private boolean tracking = false;
    private static final int REQ = 1234;

    // Tracking Data
    private float totalDistanceKm = 0f;
    private long startTimeMillis = 0;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Location lastLocation = null;
    private final float CALORIES_PER_KM = 70.0f; // Simplified Calorie estimation (kcal/km)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure this layout name matches the file created above
        setContentView(R.layout.map_track);

        // 1. UI Initialization
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);
        statusText = findViewById(R.id.statusText);
        distanceText = findViewById(R.id.distanceText);
        timeText = findViewById(R.id.timeText);
        caloriesText = findViewById(R.id.caloriesText);

        fused = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            // Optionally set map padding to avoid controls covering the map's edge (e.g., status bar)
            mMap.setPadding(0, 0, 0, 100);
        });

        // 2. Location Callback for tracking data
        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null) return;
                for(Location loc : locationResult.getLocations()) {
                    LatLng p = new LatLng(loc.getLatitude(), loc.getLongitude());
                    path.add(p);

                    // Calculate Distance
                    if (lastLocation != null) {
                        float segmentDistanceMeters = lastLocation.distanceTo(loc);
                        totalDistanceKm += (segmentDistanceMeters / 1000f); // Convert meters to km

                        // Update Calories
                        float totalCalories = totalDistanceKm * CALORIES_PER_KM;

                        // Update UI
                        distanceText.setText(String.format(Locale.getDefault(), "%.2f", totalDistanceKm));
                        caloriesText.setText(String.format(Locale.getDefault(), "%d", (int)totalCalories));
                    }
                    lastLocation = loc;

                    // Draw Path and move camera
                    mMap.addPolyline(new PolylineOptions().addAll(path));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p, 16f));
                }
            }
        };

        // 3. Timer Implementation
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTimeMillis;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;

                timeText.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000); // Repeat every second
            }
        };

        // 4. Button Click Handlers
        startBtn.setOnClickListener(v -> {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
                return;
            }
            startTracking();
        });

        stopBtn.setOnClickListener(v -> stopTracking());
    }

    private void startTracking() {
        if (tracking) return; // Prevent double start

        // Reset data
        path.clear();
        mMap.clear();
        totalDistanceKm = 0f;
        lastLocation = null;
        startTimeMillis = System.currentTimeMillis();

        tracking = true;
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        statusText.setText("Tracking...");
        timerHandler.post(timerRunnable); // Start timer

        LocationRequest req = LocationRequest.create();
        req.setInterval(2000);
        req.setFastestInterval(1000);
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fused.requestLocationUpdates(req, callback, null);
        }
    }

    private void stopTracking() {
        if (!tracking) return;

        tracking = false;
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        statusText.setText("Tracking Stopped. Distance: " + String.format(Locale.getDefault(), "%.2f KM", totalDistanceKm));

        fused.removeLocationUpdates(callback);
        timerHandler.removeCallbacks(timerRunnable); // Stop timer
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startTracking();
            else Toast.makeText(this, "Permission denied. Cannot track location.", Toast.LENGTH_SHORT).show();
        }
    }
}