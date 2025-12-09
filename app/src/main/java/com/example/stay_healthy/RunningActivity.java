package com.example.stay_healthy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector; // 引入手势识别
import android.view.Gravity;
import android.view.MotionEvent; // 引入触控事件
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AppDatabase db;
    private int seconds = 0;
    private boolean running = true;
    private String currentType = "Running";
    private double totalDistance = 0.0;
    private Location lastLocation;
    private int lastKmSeconds = 0;
    private int lastKmInt = 0;

    // UI Variables
    private TextView tvTimer, tvCalories, tvDistance, tvPace, tvAvgPace, tvAvgSpeed;
    private LinearLayout rowStats2;
    private TextView tvPageTitle;
    private TextView tabSummary, tabBreakdown;
    private View viewSummary, viewBreakdown;
    private LinearLayout layoutSplitsContainer;
    private View btnDone;
    // ❌ 删除了 btnCollapse 的引用
    private LinearLayout layoutTabs;
    private LinearLayout collapsibleContent;
    private ConstraintLayout bottomPanel; // 绑定整个底部面板

    // 手势识别
    private GestureDetector gestureDetector;

    // Map Variables
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

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
        btnDone = findViewById(R.id.btn_stop);

        tvTimer = findViewById(R.id.tv_timer);
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
        // ❌ 删除了 btnCollapse 的绑定
        bottomPanel = findViewById(R.id.bottom_panel); // 绑定整个底部面板

        btnBack.setOnClickListener(v -> finish());

        btnDone.setOnClickListener(v -> {
            running = false;
            stopLocationUpdates();
            saveWorkoutData();
        });

        // 🟢 设置手势识别
        gestureDetector = new GestureDetector(this, new CollapseGestureListener());
        bottomPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 只有当面板展开时才允许滑动收起
                if (collapsibleContent.getVisibility() == View.VISIBLE) {
                    return gestureDetector.onTouchEvent(event);
                }
                // 当面板收起时，点击任意处恢复
                if (event.getAction() == MotionEvent.ACTION_UP && collapsibleContent.getVisibility() == View.GONE) {
                    togglePanel();
                    return true;
                }
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    // 🟢 新增：手势监听器
    private class CollapseGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_VELOCITY_THRESHOLD = 1000; // 快速滑动的速度阈值

        @Override
        public boolean onDown(MotionEvent e) {
            return true; // 必须返回 true 才能接收后续事件
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffY = e2.getY() - e1.getY();

            // 只有当垂直滑动距离足够大，并且速度超过阈值时才触发
            if (Math.abs(diffY) > 100 && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY < 0) {
                    // 向上滑动 (收起)
                    if (collapsibleContent.getVisibility() == View.VISIBLE) {
                        togglePanel();
                        return true;
                    }
                } else {
                    // 向下滑动 (展开)
                    if (collapsibleContent.getVisibility() == View.GONE) {
                        togglePanel();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // 🟢 切换面板可见性 (丝滑动画)
    private void togglePanel() {
        if (collapsibleContent == null || layoutTabs == null || bottomPanel == null) return;

        // 开启平滑过渡动画
        TransitionManager.beginDelayedTransition(bottomPanel, new AutoTransition());

        if (collapsibleContent.getVisibility() == View.VISIBLE) {
            // 当前是展开状态 -> 收起
            collapsibleContent.setVisibility(View.GONE);
            layoutTabs.setVisibility(View.GONE);
            // ❌ 删除了 btnCollapse 的旋转逻辑
        } else {
            // 当前是收起状态 -> 展开 (默认展开到 SUMMARY)
            collapsibleContent.setVisibility(View.VISIBLE);
            layoutTabs.setVisibility(View.VISIBLE);
            viewSummary.setVisibility(View.VISIBLE);
            viewBreakdown.setVisibility(View.GONE);
            // ❌ 删除了 btnCollapse 的旋转逻辑
        }
    }

    // ... 其他方法保持不变 ...

    private void setupTabs() {
        if (tabSummary == null || tabBreakdown == null || bottomPanel == null) return;

        tabSummary.setOnClickListener(v -> {
            tabSummary.setTextColor(0xFFC0FF00);
            tabBreakdown.setTextColor(Color.GRAY);
            TransitionManager.beginDelayedTransition(bottomPanel, new AutoTransition());
            if (viewSummary != null) viewSummary.setVisibility(View.VISIBLE);
            if (viewBreakdown != null) viewBreakdown.setVisibility(View.GONE);
        });

        tabBreakdown.setOnClickListener(v -> {
            tabBreakdown.setTextColor(0xFFC0FF00);
            tabSummary.setTextColor(Color.GRAY);
            TransitionManager.beginDelayedTransition(bottomPanel, new AutoTransition());
            if (viewSummary != null) viewSummary.setVisibility(View.GONE);
            if (viewBreakdown != null) viewBreakdown.setVisibility(View.VISIBLE);
        });
    }

    private void addSplitRow(int kmIndex, String time) {
        if (layoutSplitsContainer == null) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 20, 0, 20);

        TextView tvKm = new TextView(this);
        tvKm.setText(kmIndex + " km");
        tvKm.setTextColor(Color.WHITE);
        tvKm.setTextSize(16);
        tvKm.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextColor(0xFFC0FF00);
        tvTime.setTextSize(16);
        tvTime.setGravity(Gravity.END);
        tvTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        row.addView(tvKm);
        row.addView(tvTime);
        layoutSplitsContainer.addView(row, 0);
    }

    // --- 地图与定位 ---
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateDistanceAndSpeed(Location currentLocation) {
        if (lastLocation != null) {
            float distanceMeters = currentLocation.distanceTo(lastLocation);
            if (distanceMeters > 2) totalDistance += (distanceMeters / 1000.0);
        }
        lastLocation = currentLocation;

        int currentKmInt = (int) totalDistance;
        if (currentKmInt > lastKmInt) {
            int secondsForThisKm = seconds - lastKmSeconds;
            lastKmSeconds = seconds;
            lastKmInt = currentKmInt;
            int pMin = secondsForThisKm / 60;
            int pSec = secondsForThisKm % 60;
            addSplitRow(currentKmInt, String.format(Locale.getDefault(), "%d:%02d", pMin, pSec));
        }

        float speedMs = currentLocation.getSpeed();
        float speedKmh = speedMs * 3.6f;
        if (speedKmh > 1.0) {
            double paceValue = 60.0 / speedKmh;
            if (tvPace != null) tvPace.setText(formatPace(paceValue));
        } else {
            if (tvPace != null) tvPace.setText("0:00");
        }

        if (tvDistance != null) tvDistance.setText(String.format(Locale.getDefault(), "%.2f", totalDistance));

        double calFactor = 60.0;
        if (currentType.equals("Cycling")) calFactor = 25.0;
        if (currentType.equals("Walking")) calFactor = 50.0;
        int calories = (int) (totalDistance * calFactor);
        if (tvCalories != null) tvCalories.setText(String.valueOf(calories));

        if (seconds > 0) {
            double avgSpeed = totalDistance / (seconds / 3600.0);
            if (tvAvgSpeed != null) tvAvgSpeed.setText(String.format(Locale.getDefault(), "%.1f", avgSpeed));
            if (avgSpeed > 1.0 && tvAvgPace != null) tvAvgPace.setText(formatPace(60.0 / avgSpeed));
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

    // --- 计时器与 UI ---
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
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                if (tvTimer != null) tvTimer.setText(time);

                if (running) {
                    if (isBallSport(currentType)) {
                        double kcalPerSec = (currentType.equals("Basketball")) ? 0.13 : 0.1;
                        int totalCal = (int) (seconds * kcalPerSec);
                        if (tvCalories != null) tvCalories.setText(String.valueOf(totalCal));
                    }
                    seconds++;
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private String formatPace(double paceValue) {
        if (paceValue > 99) return "--:--";
        int paceMin = (int) paceValue;
        int paceSec = (int) ((paceValue - paceMin) * 60);
        return String.format(Locale.getDefault(), "%d:%02d", paceMin, paceSec);
    }

    private void saveWorkoutData() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            String currentDate = "TODAY - " + sdf.format(new Date());
            String durationStr = (seconds < 60) ? seconds + " secs" : (seconds / 60) + " mins";
            String caloriesStr = (tvCalories != null ? tvCalories.getText().toString() : "0") + " kcal";
            Workout newWorkout = new Workout(currentType, durationStr, caloriesStr, currentDate);
            if (db != null) {
                db.workoutDao().insert(newWorkout);
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}