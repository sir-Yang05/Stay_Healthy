package com.example.stay_healthy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MentalWellnessPage extends AppCompatActivity {

    private TextView tvRecentEntry;
    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "MoodPrefs";
    private static final String KEY_MOOD = "todays_mood";
    private static final String KEY_DATE = "last_entry_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mental_wellness_page);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initViews();
        checkAndDisplayMood();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndDisplayMood();
    }

    private void initViews() {
        // 1. 绑定下方显示的文本框
        tvRecentEntry = findViewById(R.id.latest_diary_text);

        // 2. 绑定 "Write Diary" 按钮 (原有功能)
        View writeDiaryBtn = findViewById(R.id.write_diary_button);
        if (writeDiaryBtn != null) {
            writeDiaryBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MentalWellnessPage.this, WriteDiaryActivity.class);
                startActivity(intent);
            });
        }

        // ==========================================
        // 3. [新增] 绑定 "View History" 按钮并设置跳转
        // ==========================================
        // 注意：请确保你在 XML 里加了一个 ID 为 btn_view_history 的按钮
        View historyBtn = findViewById(R.id.btn_view_history);
        if (historyBtn != null) {
            historyBtn.setOnClickListener(v -> {
                // 跳转到历史页面 (HistoryActivity)
                // 如果 HistoryActivity 爆红，说明你还没创建这个页面，请看下面的步骤
                Intent intent = new Intent(MentalWellnessPage.this, HistoryDiaryActivity.class);
                startActivity(intent);
            });
        }

        // 4. 绑定 8 个表情图标
        setupMoodClickListener(R.id.mood_idk, "I don't know");
        setupMoodClickListener(R.id.mood_happy, "Happy");
        setupMoodClickListener(R.id.mood_neutral, "Calm");
        setupMoodClickListener(R.id.mood_sad, "Feeling Down");
        setupMoodClickListener(R.id.mood_anxious, "Anxious");
        setupMoodClickListener(R.id.mood_angry, "Angry");
        setupMoodClickListener(R.id.mood_tired, "Tired");
        setupMoodClickListener(R.id.mood_other, "Other");
    }

    private void setupMoodClickListener(int viewId, String moodName) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> saveMood(moodName));
        }
    }

    // [修改] 保存心情逻辑
    private void saveMood(String mood) {
        String currentDate = getCurrentDate(); // 获取如 "2023-10-27"

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // 1. 原有的保存 (用于显示今天的状态)
        editor.putString(KEY_MOOD, mood);
        editor.putString(KEY_DATE, currentDate);

        // 2. [新增] 同时也用日期作为 Key 存一份，为了给历史记录页面读取！
        // 这样数据格式就是: "2023-10-27" -> "Happy"
        editor.putString(currentDate, mood);

        editor.apply();

        updateDisplay(mood);
        Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show();
    }

    private void checkAndDisplayMood() {
        String savedDate = sharedPreferences.getString(KEY_DATE, "");
        String currentDate = getCurrentDate();

        if (currentDate.equals(savedDate)) {
            String savedMood = sharedPreferences.getString(KEY_MOOD, "");
            updateDisplay(savedMood);
        } else {
            resetDisplay();
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void updateDisplay(String mood) {
        if (tvRecentEntry != null) {
            tvRecentEntry.setText("Today's mood is " + mood);
            tvRecentEntry.setAlpha(1.0f);
        }
    }

    private void resetDisplay() {
        if (tvRecentEntry != null) {
            tvRecentEntry.setText("No entries for today yet~");
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