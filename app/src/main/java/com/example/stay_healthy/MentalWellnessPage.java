package com.example.stay_healthy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MentalWellnessPage extends AppCompatActivity {

    private TextView tvRecentEntry;
    private SharedPreferences sharedPreferences;

    // 定义保存数据的键名
    private static final String PREF_NAME = "MoodPrefs";
    private static final String KEY_MOOD = "todays_mood";
    private static final String KEY_DATE = "last_entry_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mental_wellness_page);

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 初始化 UI 控件
        initViews();

        // 检查今天的状态并更新 UI
        checkAndDisplayMood();

        // 底部导航栏逻辑 (保持不变)
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面（比如应用从后台切回）都检查一下日期，确保跨天时自动刷新
        checkAndDisplayMood();
    }

    private void initViews() {
        // 绑定下方显示的文本框
        tvRecentEntry = findViewById(R.id.latest_diary_text); // 请确保 XML 中有这个 ID

        // 绑定 8 个表情图标并设置点击事件
        // 注意：请确保你的 XML 中 ImageViews 的 ID 与这里一致
        setupMoodClickListener(R.id.mood_idk, "I don't know");
        setupMoodClickListener(R.id.mood_happy, "Happy");
        setupMoodClickListener(R.id.mood_neutral, "Calm");
        setupMoodClickListener(R.id.mood_sad, "Feeling Down");
        setupMoodClickListener(R.id.mood_anxious, "Anxious");
        setupMoodClickListener(R.id.mood_angry, "Angry");
        setupMoodClickListener(R.id.mood_tired, "Tired");
        setupMoodClickListener(R.id.mood_other, "Other");
    }

    // 辅助方法：为每个图标设置点击监听器
    private void setupMoodClickListener(int viewId, String moodName) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> saveMood(moodName));
        }
    }

    // 保存心情逻辑
    private void saveMood(String mood) {
        // 获取当前日期字符串 (格式: yyyy-MM-dd)
        String currentDate = getCurrentDate();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_MOOD, mood);
        editor.putString(KEY_DATE, currentDate);
        editor.apply();

        // 保存后立即更新显示
        updateDisplay(mood);
        Toast.makeText(this, "Mood saved: " + mood, Toast.LENGTH_SHORT).show();
    }

    // 检查日期并决定显示什么
    private void checkAndDisplayMood() {
        String savedDate = sharedPreferences.getString(KEY_DATE, "");
        String currentDate = getCurrentDate();

        if (currentDate.equals(savedDate)) {
            // 如果保存的日期是今天，显示心情
            String savedMood = sharedPreferences.getString(KEY_MOOD, "");
            updateDisplay(savedMood);
        } else {
            // 如果日期不匹配（比如是昨天的记录，或者是新的一天），重置显示
            resetDisplay();
        }
    }

    // 获取当前日期的方法
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // 更新 UI 显示心情
    private void updateDisplay(String mood) {
        if (tvRecentEntry != null) {
            tvRecentEntry.setText("Today's mood is " + mood);
            // 这里可以更改文字颜色或样式来表示已记录状态
            tvRecentEntry.setAlpha(1.0f);
        }
    }

    // 重置 UI 显示 (新的一天)
    private void resetDisplay() {
        if (tvRecentEntry != null) {
            tvRecentEntry.setText("No entries for today yet~");
            // 这里可以设置稍微淡一点的颜色表示空状态
            tvRecentEntry.setAlpha(0.6f);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.mental);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.exercise) {
                    startActivity(new Intent(MentalWellnessPage.this, MainPage.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                } else if (itemId == R.id.dietary) {
                    startActivity(new Intent(MentalWellnessPage.this, DietoryPage.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                } else if (itemId == R.id.mental) {
                    // 如果已经在当前页面，可以不做任何事，或者刷新
                    return true;
                } else if (itemId == R.id.profile) {
                    startActivity(new Intent(MentalWellnessPage.this, ProfileActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                }
                return false;
            }
        });
    }
}