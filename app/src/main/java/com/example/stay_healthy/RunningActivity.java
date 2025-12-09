package com.example.stay_healthy;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

        // 确保初始状态正确：running=true, 显示PAUSE, 灰色背景
        running = true;
        btnPauseResume.setText("PAUSE");
        btnPauseResume.setBackgroundTintList(ColorStateList.valueOf(COLOR_GRAY));
        btnPauseResume.setTextColor(COLOR_WHITE);
        // 初始化时，记录计时起始时间
        startTimeMillis = System.currentTimeMillis();

        // DONE 按钮逻辑 (停止并保存)
        btnDone.setOnClickListener(v -> {
            running = false;
            stopLocationUpdates();
            saveWorkoutData();
        });

        // PAUSE/RESUME 按钮逻辑 (使用系统时间)
        btnPauseResume.setOnClickListener(v -> {
            if (running) {
                // 当前正在运行 -> 暂停
                running = false;
                stopLocationUpdates();
                btnPauseResume.setText("RESUME");
                btnPauseResume.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mint_green));
                btnPauseResume.setTextColor(COLOR_BLACK);

                // 暂停时：将已用时间保存到 totalTimeMillis，并重置 startTimeMillis
                totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
                startTimeMillis = 0;
            } else {
                // 当前已暂停 -> 继续
                running = true;
                startLocationUpdates();
                // 重新设置起始时间 = 当前系统时间 - 已用时间
                startTimeMillis = System.currentTimeMillis() - totalTimeMillis;
                runTimer(); // 重新启动计时器循环
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

        if (running && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    // 核心计时器和数据更新
    private void updateDistanceAndSpeed(Location currentLocation) {
        // totalSeconds 依赖于 totalTimeMillis 的计算
        long totalSeconds = totalTimeMillis / 1000;

        if (lastLocation != null) {
            float distanceMeters = currentLocation.distanceTo(lastLocation);
            if (distanceMeters > 2) totalDistance += (distanceMeters / 1000.0);
        }
        lastLocation = currentLocation;

        // --- 分段数据 ---
        int currentKmInt = (int) totalDistance;
        if (currentKmInt > lastKmInt && totalSeconds > lastKmSeconds) {
            int secondsForThisKm = (int) totalSeconds - lastKmSeconds;
            lastKmSeconds = (int) totalSeconds;
            lastKmInt = currentKmInt;

            // 记录时，使用 min:ss 格式 (Breakdown)
            int pMin = secondsForThisKm / 60;
            int pSec = secondsForThisKm % 60;
            String paceTime = String.format(Locale.getDefault(), "%d:%02d", pMin, pSec);
            addSplitRow(currentKmInt, paceTime);
        }

        // --- 实时速度/配速 (s/m 和 m/s) ---
        double speedMs = currentLocation.getSpeed();

        if (speedMs > 0.1) {
            double paceValueSM = 1.0 / speedMs;
            if (tvPace != null) tvPace.setText(String.format(Locale.getDefault(), "%.1f", paceValueSM));
        } else {
            if (tvPace != null) tvPace.setText("0.0");
        }

        // --- 更新距离和卡路里 ---
        if (tvDistance != null) tvDistance.setText(String.format(Locale.getDefault(), "%.2f", totalDistance));
        double calFactor = 60.0;
        if (currentType.equals("Cycling")) calFactor = 25.0;
        if (currentType.equals("Walking")) calFactor = 50.0;
        int calories = (int) (totalDistance * calFactor);
        if (tvCalories != null) tvCalories.setText(String.valueOf(calories));

        // --- 平均速度/配速 (m/s 和 s/m) ---
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
            // 球类运动隐藏距离和配速相关数据
            if (tvDistance != null && ((View)tvDistance.getParent()).getVisibility() != View.GONE) ((View)tvDistance.getParent()).setVisibility(View.GONE);
            if (tvPace != null && ((View)tvPace.getParent()).getVisibility() != View.GONE) ((View)tvPace.getParent()).setVisibility(View.GONE);
            if (rowStats2 != null) rowStats2.setVisibility(View.GONE);
            if (tabBreakdown != null) tabBreakdown.setVisibility(View.GONE);
        }
    }

    private boolean isBallSport(String type) {
        return type.equals("Basketball") || type.equals("Badminton");
    }

    // 🟢 核心计时器逻辑 (基于系统时间)
    private void runTimer() {
        if (!running) return;

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (running) {
                    // 1. 基于当前系统时间计算流逝的总时间
                    long currentTime = System.currentTimeMillis();
                    totalTimeMillis = currentTime - startTimeMillis;

                    long totalSeconds = totalTimeMillis / 1000;

                    // 2. 时间分解
                    int hours = (int) totalSeconds / 3600;
                    int minutes = (int) (totalSeconds % 3600) / 60;
                    int secs = (int) totalSeconds % 60;
                    // 计算两位数的毫秒 (00-99)
                    int ms = (int) (totalTimeMillis % 1000) / 10;

                    // ⚠️ 格式化时间：将秒后的冒号从 tvTimerMain 中移出，留给 tvMilliseconds
                    String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                    // 🟢 毫秒格式：在前面添加冒号 (使用小字体 20sp)
                    String msString = String.format(Locale.getDefault(), ":%02d", ms);

                    if (tvTimerMain != null) tvTimerMain.setText(time);
                    if (tvMilliseconds != null) tvMilliseconds.setText(msString);

                    // 运动消耗 (球类依然按时间计算)
                    if (isBallSport(currentType)) {
                        double kcalPerSec = (currentType.equals("Basketball")) ? 0.13 : 0.1;
                        int totalCal = (int) (totalSeconds * kcalPerSec);
                        if (tvCalories != null) tvCalories.setText(String.valueOf(totalCal));
                    }

                    handler.postDelayed(this, 10); // 10ms 间隔 (用于平滑刷新)
                }
            }
        });
    }

    private String formatPace(double paceValue) {
        return String.format(Locale.getDefault(), "%.1f", paceValue);
    }

    private void saveWorkoutData() {
        long finalSeconds = totalTimeMillis / 1000;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            String currentDate = "TODAY - " + sdf.format(new Date());
            String durationStr = (finalSeconds < 60) ? finalSeconds + " secs" : (finalSeconds / 60) + " mins";
            String caloriesStr = (tvCalories != null ? tvCalories.getText().toString() : "0") + " kcal";
            Workout newWorkout = new Workout(currentType, durationStr, caloriesStr, currentDate);

            if (db != null) {
                // 修复: 使用正确的 DAO 方法名 workoutDao()
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