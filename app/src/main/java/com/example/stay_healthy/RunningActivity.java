package com.example.stay_healthy; // ⚠️ CHECK YOUR PACKAGE NAME!

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AppDatabase db;

    // Timer Variables
    private int seconds = 0;
    private boolean running = true;

    // Data Variables
    private double totalDistance = 0.0;
    private String currentType = "Running"; // Default

    // UI Components
    private TextView tvTimer, tvCalories, tvDistance, tvPace, tvAvgPace, tvAvgSpeed;
    private LinearLayout rowStats2; // Container for the second row of stats

    // Map & Location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        // Hide the default Action Bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Initialize Database
        db = AppDatabase.getInstance(this);

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get Sport Type passed from HomeFragment
        if (getIntent().hasExtra("SPORT_TYPE")) {
            currentType = getIntent().getStringExtra("SPORT_TYPE");
        }

        // Bind Views
        ImageView btnBack = findViewById(R.id.btn_back_run);
        Button btnDone = findViewById(R.id.btn_stop);

        tvTimer = findViewById(R.id.tv_timer);
        tvCalories = findViewById(R.id.tv_calories_run);
        tvDistance = findViewById(R.id.tv_distance);
        tvPace = findViewById(R.id.tv_pace);
        tvAvgPace = findViewById(R.id.tv_avg_pace);
        tvAvgSpeed = findViewById(R.id.tv_avg_speed);
        rowStats2 = findViewById(R.id.row_stats_2);

        // Load Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup UI based on Sport Type (Hide distance for Basketball etc.)
        setupUIForSport(currentType);

        // Start Timer
        runTimer();

        // Button Listeners
        btnBack.setOnClickListener(v -> finish());

        btnDone.setOnClickListener(v -> {
            running = false;
            stopLocationUpdates(); // Stop GPS to save battery
            saveWorkoutData();
        });
    }

    // --- Map Logic ---

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkPermissionAndEnableLocation();
    }

    private void checkPermissionAndEnableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        try {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true); // Show Blue Dot
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                startLocationUpdates();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    LatLng currentLatLng = new LatLng(lat, lng);

                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission required for map", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- UI & Timer Logic ---

    private void setupUIForSport(String type) {
        boolean isDistanceSport = type.equals("Running") || type.equals("Walking") || type.equals("Cycling");

        // If it's NOT a distance sport (like Basketball), hide distance/pace fields
        if (!isDistanceSport) {
            if (tvDistance != null && tvDistance.getParent() instanceof View) {
                ((View) tvDistance.getParent()).setVisibility(View.GONE);
            }
            if (tvPace != null && tvPace.getParent() instanceof View) {
                ((View) tvPace.getParent()).setVisibility(View.GONE);
            }
            if (rowStats2 != null) rowStats2.setVisibility(View.GONE);
        }
    }

    private void runTimer() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Random random = new Random();

        handler.post(new Runnable() {
            @Override
            public void run() {
                // Update Time
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                if (tvTimer != null) tvTimer.setText(time);

                if (running) {
                    // Logic: Calculate Data based on sport type
                    if (currentType.equals("Basketball") || currentType.equals("Badminton")) {
                        // Time-based calculation
                        double kcalPerSec = (currentType.equals("Basketball")) ? 0.13 : 0.1;
                        int totalCal = (int) (seconds * kcalPerSec);
                        if (tvCalories != null) tvCalories.setText(String.valueOf(totalCal));
                    } else {
                        // Distance-based calculation
                        double speedFactor = 1.0;
                        if (currentType.equals("Walking")) speedFactor = 0.6;
                        if (currentType.equals("Cycling")) speedFactor = 2.5;

                        // Simulate speed variation
                        double currentSpeedKmh = (8.0 + (random.nextDouble() * 4.0)) * speedFactor;
                        double distanceThisSecond = currentSpeedKmh / 3600.0;
                        totalDistance += distanceThisSecond;

                        if (tvDistance != null) tvDistance.setText(String.format(Locale.getDefault(), "%.2f", totalDistance));

                        double calFactor = 60.0;
                        if (currentType.equals("Cycling")) calFactor = 25.0;
                        if (currentType.equals("Walking")) calFactor = 50.0;
                        int calories = (int) (totalDistance * calFactor);
                        if (tvCalories != null) tvCalories.setText(String.valueOf(calories));

                        double currentPaceVal = 60.0 / currentSpeedKmh;
                        if (tvPace != null) tvPace.setText(formatPace(currentPaceVal));

                        double avgSpeedVal = 0;
                        if (seconds > 0) avgSpeedVal = totalDistance / (seconds / 3600.0);
                        if (tvAvgSpeed != null) tvAvgSpeed.setText(String.format(Locale.getDefault(), "%.1f", avgSpeedVal));

                        if (avgSpeedVal > 0) {
                            double avgPaceVal = 60.0 / avgSpeedVal;
                            if (tvAvgPace != null) tvAvgPace.setText(formatPace(avgPaceVal));
                        }
                    }
                    seconds++;
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private String formatPace(double paceValue) {
        int paceMin = (int) paceValue;
        int paceSec = (int) ((paceValue - paceMin) * 60);
        return String.format(Locale.getDefault(), "%d:%02d", paceMin, paceSec);
    }

    private void saveWorkoutData() {
        try {
            // Use simple date format to avoid 'Illegal pattern character' error
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            String currentDate = "TODAY - " + sdf.format(new Date());

            String durationStr = (seconds < 60) ? seconds + " secs" : (seconds / 60) + " mins";
            String caloriesRaw = (tvCalories != null) ? tvCalories.getText().toString() : "0";
            String caloriesStr = caloriesRaw + " kcal";

            Workout newWorkout = new Workout(currentType, durationStr, caloriesStr, currentDate);

            if (db != null) {
                db.workoutDao().insert(newWorkout);
                Toast.makeText(this, "Workout Saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error: Database not initialized", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}