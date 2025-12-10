package com.example.stay_healthy; // ⚠️ 确认包名

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;
import androidx.transition.AutoTransition;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AppDatabase db;
    // 存储已用时间 (暂停时保存)
    private long totalTimeMillis = 0;
    // 记录计时开始时的系统时间戳 (用于精确计算)
    private long startTimeMillis = 0;

    private boolean running = true;
    private String currentType = "Running";
    private double totalDistance = 0.0;
    private Location lastLocation;
    private int lastKmSeconds = 0;
    private int lastKmInt = 0;

    // UI Variables
    private TextView tvTimerMain, tvMilliseconds;
    private TextView tvCalories, tvDistance, tvPace, tvAvgPace, tvAvgSpeed;
    private LinearLayout rowStats2;
    private TextView tvPageTitle;
    private TextView tabSummary, tabBreakdown;
    private View viewSummary, viewBreakdown;
    private LinearLayout layoutSplitsContainer;
    private Button btnDone;
    private Button btnPauseResume;
    private ImageView btnCollapse;
    private LinearLayout layoutTabs;
    private LinearLayout collapsibleContent;
    private View bottomPanel;

    // Map Variables
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // 颜色常量
    private final int COLOR_GRAY = Color.parseColor("#808080");
    private final int COLOR_WHITE = Color.parseColor("#FFFFFF");
    private final int COLOR_BLACK = Color.parseColor("#000000");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        db = AppDatabase.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (getIntent().hasExtra("SPORT_TYPE")) {
            currentType = getIntent().getStringExtra("SPORT_TYPE");
        }

        initViews();
        setupUIForSport(currentType);
        setupTabs();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        runTimer();
    }

    private void initViews() {
        tvPageTitle = findViewById(R.id.tv_page_title);
        if (tvPageTitle != null) tvPageTitle.setVisibility(View.GONE);

        ImageView btnBack = findViewById(R.id.btn_back_run);

        tvTimerMain = findViewById(R.id.tv_timer_main);
        tvMilliseconds = findViewById(R.id.tv_milliseconds);

        btnDone = findViewById(R.id.btn_stop);
        btnPauseResume = findViewById(R.id.btn_pause_resume);

        tvCalories = findViewById(R.id.tv_calories_run);
        tvDistance = findViewById(R.id.tv_distance);
        tvPace = findViewById(R.id.tv_pace);
        tvAvgPace = findViewById(R.id.tv_avg_pace);
        tvAvgSpeed = findViewById(R.id.tv_avg_speed);
        rowStats2 = findViewById(R.id.row_stats_2);

        tabSummary = findViewById(R.id.tab_summary);
        tabBreakdown = findViewById(R.id.tab_breakdown);
        viewSummary = findViewById(R.id.view_summary);
        viewBreakdown = findViewById(R.id.view_breakdown);
        layoutSplitsContainer = findViewById(R.id.layout_splits_container);

        layoutTabs = findViewById(R.id.layout_tabs);
        collapsibleContent = findViewById(R.id.collapsible_content);

        btnCollapse = findViewById(R.id.btn_collapse);
        bottomPanel = findViewById(R.id.bottom_panel);

        btnBack.setOnClickListener(v -> finish());

        btnCollapse.setOnClickListener(v -> togglePanel());

        // 确保初始状态正确
        running = true;
        btnPauseResume.setText("PAUSE");
        btnPauseResume.setBackgroundTintList(ColorStateList.valueOf(COLOR_GRAY));
        btnPauseResume.setTextColor(COLOR_WHITE);
        startTimeMillis = System.currentTimeMillis();

        // DONE 按钮逻辑
        btnDone.setOnClickListener(v -> {
            running = false;
            stopLocationUpdates();
            saveWorkoutData();
        });

        // PAUSE/RESUME 按钮逻辑
        btnPauseResume.setOnClickListener(v -> {
            if (running) {
                running = false;
                stopLocationUpdates();
                btnPauseResume.setText("RESUME");
                btnPauseResume.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mint_green));
                btnPauseResume.setTextColor(COLOR_BLACK);

                totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
                startTimeMillis = 0;
            } else {
                running = true;
                startLocationUpdates();
                startTimeMillis = System.currentTimeMillis() - totalTimeMillis;
                runTimer();
                btnPauseResume.setText("PAUSE");
                btnPauseResume.setBackgroundTintList(ColorStateList.valueOf(COLOR_GRAY));
                btnPauseResume.setTextColor(COLOR_WHITE);
            }
        });
    }

    private void togglePanel() {
        if (collapsibleContent == null || layoutTabs == null || btnCollapse == null || bottomPanel == null) return;
        TransitionManager.beginDelayedTransition((ViewGroup) bottomPanel, new AutoTransition());
        if (collapsibleContent.getVisibility() == View.VISIBLE) {
            collapsibleContent.setVisibility(View.GONE);
            layoutTabs.setVisibility(View.GONE);
            btnCollapse.setRotation(270);
        } else {
            collapsibleContent.setVisibility(View.VISIBLE);
            layoutTabs.setVisibility(View.VISIBLE);
            btnCollapse.setRotation(90);
            if(viewSummary.getVisibility() == View.GONE && viewBreakdown.getVisibility() == View.GONE) {
                viewSummary.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupTabs() {
        if (tabSummary == null || tabBreakdown == null || bottomPanel == null) return;
        tabSummary.setOnClickListener(v -> {
            tabSummary.setTextColor(0xFFC0FF00);
            tabBreakdown.setTextColor(Color.GRAY);
            TransitionManager.beginDelayedTransition((ViewGroup) bottomPanel, new AutoTransition());
            if (viewSummary != null) viewSummary.setVisibility(View.VISIBLE);
            if (viewBreakdown != null) viewBreakdown.setVisibility(View.GONE);
        });
        tabBreakdown.setOnClickListener(v -> {
            tabBreakdown.setTextColor(0xFFC0FF00);
            tabSummary.setTextColor(Color.GRAY);
            TransitionManager.beginDelayedTransition((ViewGroup) bottomPanel, new AutoTransition());
            if (viewSummary != null) viewSummary.setVisibility(View.GONE);
            if (viewBreakdown != null) viewBreakdown.setVisibility(View.VISIBLE);
        });
    }

    private void addSplitRow(int kmIndex, String paceTime) {
        if (layoutSplitsContainer == null) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 20, 0, 20);
        TextView tvKm = new TextView(this);
        tvKm.setText(kmIndex + " km");
        tvKm.setTextColor(Color.WHITE);
        tvKm.setTextSize(16);
        row.addView(tvKm);
        TextView tvTime = new TextView(this);
        tvTime.setText(paceTime);
        tvTime.setTextColor(0xFFC0FF00);
        tvTime.setTextSize(16);
        tvTime.setGravity(Gravity.END);
        row.addView(tvTime);
        layoutSplitsContainer.addView(row, 0);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkPermissionAndEnableLocation();
    }

    private void checkPermissionAndEnableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        try {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                startLocationUpdates();
            }
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000).build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!running) return;
                if (locationResult.getLastLocation() != null) {
                    Location currentLocation = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    if (mMap != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
                    if (isBallSport(currentType)) return;
                    updateDistanceAndSpeed(currentLocation);
                }
            }
        };
        if (running && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateDistanceAndSpeed(Location currentLocation) {
        long totalSeconds = totalTimeMillis / 1000;
        if (lastLocation != null) {
            float distanceMeters = currentLocation.distanceTo(lastLocation);
            if (distanceMeters > 2) totalDistance += (distanceMeters / 1000.0);
        }
        lastLocation = currentLocation;

        int currentKmInt = (int) totalDistance;
        if (currentKmInt > lastKmInt && totalSeconds > lastKmSeconds) {
            int secondsForThisKm = (int) totalSeconds - lastKmSeconds;
            lastKmSeconds = (int) totalSeconds;
            lastKmInt = currentKmInt;
            int pMin = secondsForThisKm / 60;
            int pSec = secondsForThisKm % 60;
            String paceTime = String.format(Locale.getDefault(), "%d:%02d", pMin, pSec);
            addSplitRow(currentKmInt, paceTime);
        }

        float speedMs = currentLocation.getSpeed();
        if (speedMs > 0.1) {
            double paceValueSM = 1.0 / speedMs;
            if (tvPace != null) tvPace.setText(String.format(Locale.getDefault(), "%.1f", paceValueSM));
        } else {
            if (tvPace != null) tvPace.setText("0.0");
        }

        if (tvDistance != null) tvDistance.setText(String.format(Locale.getDefault(), "%.2f", totalDistance));
        double calFactor = 60.0;
        if (currentType.equals("Cycling")) calFactor = 25.0;
        if (currentType.equals("Walking")) calFactor = 50.0;
        int calories = (int) (totalDistance * calFactor);
        if (tvCalories != null) tvCalories.setText(String.valueOf(calories));

        if (totalSeconds > 0) {
            double avgSpeedKmh = totalDistance / (totalSeconds / 3600.0);
            double avgSpeedMs = avgSpeedKmh / 3.6;
            if (tvAvgSpeed != null) tvAvgSpeed.setText(String.format(Locale.getDefault(), "%.1f", avgSpeedMs));
            if (avgSpeedMs > 0.1) {
                double avgPaceSM = 1.0 / avgSpeedMs;
                if (tvAvgPace != null) tvAvgPace.setText(String.format(Locale.getDefault(), "%.1f", avgPaceSM));
            } else {
                if (tvAvgPace != null) tvAvgPace.setText("0.0");
            }
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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }

    private void setupUIForSport(String type) {
        if (isBallSport(type)) {
            if (tvDistance != null && ((View)tvDistance.getParent()).getVisibility() != View.GONE) ((View)tvDistance.getParent()).setVisibility(View.GONE);
            if (tvPace != null && ((View)tvPace.getParent()).getVisibility() != View.GONE) ((View)tvPace.getParent()).setVisibility(View.GONE);
            if (rowStats2 != null) rowStats2.setVisibility(View.GONE);
            if (tabBreakdown != null) tabBreakdown.setVisibility(View.GONE);
        }
    }

    private boolean isBallSport(String type) {
        return type.equals("Basketball") || type.equals("Badminton");
    }

    private void runTimer() {
        if (!running) return;
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (running) {
                    long currentTime = System.currentTimeMillis();
                    totalTimeMillis = currentTime - startTimeMillis;
                    long totalSeconds = totalTimeMillis / 1000;

                    int hours = (int) totalSeconds / 3600;
                    int minutes = (int) (totalSeconds % 3600) / 60;
                    int secs = (int) totalSeconds % 60;
                    int ms = (int) (totalTimeMillis % 1000) / 10;

                    String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                    String msString = String.format(Locale.getDefault(), ".%02d", ms);

                    if (tvTimerMain != null) tvTimerMain.setText(time);
                    if (tvMilliseconds != null) tvMilliseconds.setText(msString);

                    if (isBallSport(currentType)) {
                        double kcalPerSec = (currentType.equals("Basketball")) ? 0.13 : 0.1;
                        int totalCal = (int) (totalSeconds * kcalPerSec);
                        if (tvCalories != null) tvCalories.setText(String.valueOf(totalCal));
                    }
                    handler.postDelayed(this, 10);
                }
            }
        });
    }

    private String formatPace(double paceValue) {
        return String.format(Locale.getDefault(), "%.1f", paceValue);
    }

    // 🟢 核心修改：保存到 Firebase (云端) + Room (本地)
    private void saveWorkoutData() {
        long finalSeconds = totalTimeMillis / 1000;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            String currentDate = "TODAY - " + sdf.format(new Date());
            String durationStr = (finalSeconds < 60) ? finalSeconds + " secs" : (finalSeconds / 60) + " mins";
            String caloriesStr = (tvCalories != null ? tvCalories.getText().toString() : "0") + " kcal";

            // 1. 本地保存
            Workout newWorkout = new Workout(currentType, durationStr, caloriesStr, currentDate);
            if (db != null) db.workoutDao().insert(newWorkout);

            // 2. 🟢 云端保存 (Firebase)
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Map<String, Object> workoutMap = new HashMap<>();
                workoutMap.put("type", currentType);
                workoutMap.put("duration", durationStr);
                workoutMap.put("calories", caloriesStr);
                workoutMap.put("date", currentDate);
                workoutMap.put("timestamp", System.currentTimeMillis());

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .collection("workouts")
                        .add(workoutMap)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Saved to Cloud!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Cloud Sync Failed", Toast.LENGTH_SHORT).show();
                        });
            }

            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}