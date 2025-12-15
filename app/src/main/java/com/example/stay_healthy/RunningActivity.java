package com.example.stay_healthy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager; // ✨ 1. 导入动画库
import android.view.View;
import android.view.ViewGroup; // ✨ 2. 导入 ViewGroup
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<LatLng> pathPoints = new ArrayList<>();
    private Polyline polyline;

    private TextView tvTimerMain, tvMilliseconds;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updateTime = 0L;
    private boolean isRunning = false;

    private TextView tvDistance, tvPace, tvCalories;
    private float totalDistance = 0f;

    private Button btnStartPause, btnStop, btnReset;
    private LinearLayout layoutButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        tvTimerMain = findViewById(R.id.tv_timer_main);
        tvMilliseconds = findViewById(R.id.tv_milliseconds);
        tvDistance = findViewById(R.id.tv_distance);
        tvPace = findViewById(R.id.tv_pace);
        tvCalories = findViewById(R.id.tv_calories_run);

        btnStartPause = findViewById(R.id.btn_pause_resume);
        btnStop = findViewById(R.id.btn_stop);
        btnReset = findViewById(R.id.btn_reset);
        layoutButtons = findViewById(R.id.layout_buttons);

        findViewById(R.id.btn_back_run).setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupButtons();
        createLocationCallback();
    }

    private void setupButtons() {
        btnStartPause.setOnClickListener(v -> {
            if (!isRunning) {
                startRun();
            } else {
                pauseRun();
            }
        });

        btnStop.setOnClickListener(v -> stopRun());

        btnReset.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(layoutButtons);

            pauseRun();
            resetData();
            btnReset.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            btnStartPause.setText("START");
        });
    }

    private void startRun() {
        isRunning = true;
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(updateTimerThread, 0);
        TransitionManager.beginDelayedTransition(layoutButtons);

        btnStartPause.setText("PAUSE");
        btnStop.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);

        startLocationUpdates();
    }

    private void pauseRun() {
        isRunning = false;
        timeSwapBuff += timeInMilliseconds;
        timerHandler.removeCallbacks(updateTimerThread);

        TransitionManager.beginDelayedTransition(layoutButtons);

        btnStartPause.setText("RESUME");
        btnStop.setVisibility(View.VISIBLE);
        btnReset.setVisibility(View.VISIBLE);

        stopLocationUpdates();
    }

    private void stopRun() {
        pauseRun();
        new AlertDialog.Builder(this)
                .setTitle("End Run?")
                .setMessage("Are you sure you want to end this run?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    saveRunData();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void resetData() {
        timeSwapBuff = 0L;
        timeInMilliseconds = 0L;
        startTime = 0L;
        updateTime = 0L;
        totalDistance = 0f;
        pathPoints.clear();
        if (polyline != null) polyline.remove();
        polyline = null;

        updateTimerUI(0, 0, 0, 0);
        tvDistance.setText("0.00");
        tvPace.setText("0.0");
        tvCalories.setText("0");
    }

    private void saveRunData() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String currentDate = sdfDate.format(new Date());

        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.US);
        String currentTime = sdfTime.format(new Date());

        String durationStr = tvTimerMain.getText().toString();
        String distanceStr = tvDistance.getText().toString() + " km";
        String caloriesStr = tvCalories.getText().toString() + " kcal";
        String paceStr = tvPace.getText().toString();

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                Workout workout = new Workout();
                workout.type = "Running";
                workout.date = currentDate;
                workout.time = currentTime;
                workout.duration = durationStr;
                workout.distance = distanceStr;
                workout.calories = caloriesStr;
                workout.pace = paceStr;

                db.workoutDao().insert(workout);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Workout Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
                            } else {
                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener(this, currentLocation -> {
                                            if (currentLocation != null) {
                                                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
                                            }
                                        });
                            }
                        });
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission needed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isRunning) return;

                for (Location location : locationResult.getLocations()) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (!pathPoints.isEmpty()) {
                        Location lastLoc = new Location("");
                        lastLoc.setLatitude(pathPoints.get(pathPoints.size() - 1).latitude);
                        lastLoc.setLongitude(pathPoints.get(pathPoints.size() - 1).longitude);
                        float dist = location.distanceTo(lastLoc);
                        if (dist < 2) continue;
                        totalDistance += dist;
                    }

                    pathPoints.add(currentLatLng);
                    drawPolyline();
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                    updateStats();
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateDistanceMeters(2)
                .build();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void drawPolyline() {
        if (polyline != null) {
            polyline.setPoints(pathPoints);
        } else {
            PolylineOptions options = new PolylineOptions()
                    .addAll(pathPoints)
                    .width(15)
                    .color(0xFF00FF00);
            polyline = mMap.addPolyline(options);
        }
    }

    private void updateStats() {
        tvDistance.setText(String.format(Locale.US, "%.2f", totalDistance / 1000f));
        int calories = (int) (60 * (totalDistance / 1000f) * 1.036);
        tvCalories.setText(String.valueOf(calories));

        if (totalDistance > 0) {
            long totalSeconds = timeSwapBuff / 1000;
            double minutes = totalSeconds / 60.0;
            double km = totalDistance / 1000.0;
            if (km > 0.001) {
                double pace = minutes / km;
                tvPace.setText(String.format(Locale.US, "%.1f", pace));
            }
        }
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = System.currentTimeMillis() - startTime;
            updateTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updateTime / 1000);
            int mins = secs / 60;
            int hrs = mins / 60;
            secs = secs % 60;
            mins = mins % 60;
            int milliseconds = (int) (updateTime % 1000);

            updateTimerUI(hrs, mins, secs, milliseconds);
            timerHandler.postDelayed(this, 0);
        }
    };

    private void updateTimerUI(int hrs, int mins, int secs, int ms) {
        String timerStr = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs);
        tvTimerMain.setText(timerStr);
        tvMilliseconds.setText(String.format(Locale.US, ".%02d", ms / 10));
    }
}