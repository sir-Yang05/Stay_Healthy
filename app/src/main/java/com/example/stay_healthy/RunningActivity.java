package com.example.stay_healthy;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class RunningActivity extends AppCompatActivity {

    private AppDatabase db;
    private int seconds = 0;
    private boolean running = true;
    private double totalDistance = 0.0;
    private String currentType = "Running";

    // 记录上一公里的时间点，用于计算分段配速
    private int lastKmSeconds = 0;
    private int currentKmIndex = 1;

    // 控件
    private TextView tvTimer, tvCalories, tvDistance, tvPace, tvAvgPace, tvAvgSpeed;
    private LinearLayout rowStats2;
    private TextView tvPageTitle;

    // Tab 相关控件
    private TextView tabSummary, tabBreakdown;
    private View viewSummary, viewBreakdown;
    private LinearLayout layoutSplitsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        db = AppDatabase.getInstance(this);

        if (getIntent().hasExtra("SPORT_TYPE")) {
            currentType = getIntent().getStringExtra("SPORT_TYPE");
        }

        initViews();
        setupUIForSport(currentType);
        setupTabs(); // 初始化 Tab 切换功能

        runTimer();
    }

    private void initViews() {
        tvPageTitle = findViewById(R.id.tv_page_title);
        tvPageTitle.setText("TRACK " + currentType.toUpperCase());

        ImageView btnBack = findViewById(R.id.btn_back_run);
        Button btnDone = findViewById(R.id.btn_stop);

        tvTimer = findViewById(R.id.tv_timer);
        tvCalories = findViewById(R.id.tv_calories_run);
        tvDistance = findViewById(R.id.tv_distance);
        tvPace = findViewById(R.id.tv_pace);
        tvAvgPace = findViewById(R.id.tv_avg_pace);
        tvAvgSpeed = findViewById(R.id.tv_avg_speed);
        rowStats2 = findViewById(R.id.row_stats_2);

        // Tab 控件
        tabSummary = findViewById(R.id.tab_summary);
        tabBreakdown = findViewById(R.id.tab_breakdown);
        viewSummary = findViewById(R.id.view_summary);
        viewBreakdown = findViewById(R.id.view_breakdown);
        layoutSplitsContainer = findViewById(R.id.layout_splits_container);

        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> {
            running = false;
            saveWorkoutData();
        });
    }

    // 设置 Tab 点击切换逻辑
    private void setupTabs() {
        // 点击 Summary
        tabSummary.setOnClickListener(v -> {
            // 变绿
            tabSummary.setTextColor(0xFFC0FF00); // 荧光绿
            tabBreakdown.setTextColor(Color.GRAY);
            // 切换视图
            viewSummary.setVisibility(View.VISIBLE);
            viewBreakdown.setVisibility(View.GONE);

            // 调整按钮位置 (让它回到 Summary 下面)
            View btnStop = findViewById(R.id.btn_stop);
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnStop.getLayoutParams();
            params.topToBottom = R.id.view_summary;
            btnStop.setLayoutParams(params);
        });

        // 点击 Breakdown
        tabBreakdown.setOnClickListener(v -> {
            // 变绿
            tabBreakdown.setTextColor(0xFFC0FF00);
            tabSummary.setTextColor(Color.GRAY);
            // 切换视图
            viewSummary.setVisibility(View.GONE);
            viewBreakdown.setVisibility(View.VISIBLE);

            // 调整按钮位置 (让它跑到 Breakdown 下面)
            View btnStop = findViewById(R.id.btn_stop);
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnStop.getLayoutParams();
            params.topToBottom = R.id.view_breakdown;
            btnStop.setLayoutParams(params);
        });
    }

    // 动态添加一行“分段数据”
    private void addSplitRow(int kmIndex, String time) {
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
        tvTime.setTextColor(0xFFC0FF00); // 绿色
        tvTime.setTextSize(16);
        tvTime.setGravity(Gravity.END);
        tvTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        row.addView(tvKm);
        row.addView(tvTime);
        layoutSplitsContainer.addView(row, 0); // 加到最上面
    }

    private void setupUIForSport(String type) {
        boolean isDistanceSport = type.equals("Running") || type.equals("Walking") || type.equals("Cycling");
        if (!isDistanceSport) {
            if (tvDistance != null && tvDistance.getParent() instanceof View) {
                ((View) tvDistance.getParent()).setVisibility(View.GONE);
            }
            if (tvPace != null && tvPace.getParent() instanceof View) {
                ((View) tvPace.getParent()).setVisibility(View.GONE);
            }
            if (rowStats2 != null) rowStats2.setVisibility(View.GONE);

            // 如果不是距离运动，隐藏 Breakdown Tab (因为没有分段)
            if (tabBreakdown != null) tabBreakdown.setVisibility(View.GONE);
        }
    }

    private void runTimer() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Random random = new Random();

        handler.post(new Runnable() {
            @Override
            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                if (tvTimer != null) tvTimer.setText(time);

                if (running) {
                    if (currentType.equals("Basketball") || currentType.equals("Badminton")) {
                        double kcalPerSec = (currentType.equals("Basketball")) ? 0.13 : 0.1;
                        int totalCal = (int) (seconds * kcalPerSec);
                        if (tvCalories != null) tvCalories.setText(String.valueOf(totalCal));
                    } else {
                        double speedFactor = 1.0;
                        if (currentType.equals("Walking")) speedFactor = 0.6;
                        if (currentType.equals("Cycling")) speedFactor = 2.5;

                        double currentSpeedKmh = (8.0 + (random.nextDouble() * 4.0)) * speedFactor;
                        double distanceThisSecond = currentSpeedKmh / 3600.0;

                        // 记录之前的整数距离 (比如 1.9km)
                        int oldKm = (int) totalDistance;

                        totalDistance += distanceThisSecond;

                        // 记录现在的整数距离 (比如 2.0km)
                        int newKm = (int) totalDistance;

                        // ★★★ 如果整数部分增加了 (说明跑满了一公里) ★★★
                        if (newKm > oldKm) {
                            // 计算这一公里用了多久
                            int secondsForThisKm = seconds - lastKmSeconds;
                            lastKmSeconds = seconds;

                            // 格式化时间 05:30
                            int pMin = secondsForThisKm / 60;
                            int pSec = secondsForThisKm % 60;
                            String splitTime = String.format(Locale.getDefault(), "%d:%02d", pMin, pSec);

                            // 添加到 Breakdown 列表
                            addSplitRow(newKm, splitTime);
                        }

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