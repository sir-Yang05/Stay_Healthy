package com.example.stay_healthy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MentalWellnessPage extends Fragment {

    private TextView tvRecentEntry;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MoodPrefs";
    private static final String KEY_MOOD = "todays_mood";
    private static final String KEY_DATE = "last_entry_date";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. 这里的 layout 文件名要和你朋友的一致 (可能是 mental_wellness_page)
        View view = inflater.inflate(R.layout.mental_wellness_page, container, false);

        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        initViews(view);
        checkAndDisplayMood();

        return view;
    }

    private void initViews(View view) {
        // ✅ 关键修复：Fragment 里必须用 view.findViewById
        tvRecentEntry = view.findViewById(R.id.latest_diary_text);

        View writeDiaryBtn = view.findViewById(R.id.write_diary_button);
        if (writeDiaryBtn != null) {
            writeDiaryBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WriteDiaryActivity.class);
                startActivity(intent);
            });
        }

        View historyBtn = view.findViewById(R.id.btn_view_history);
        if (historyBtn != null) {
            historyBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), HistoryDiaryActivity.class);
                startActivity(intent);
            });
        }

        setupMoodClickListener(view, R.id.mood_idk, "I don't know");
        setupMoodClickListener(view, R.id.mood_happy, "Happy");
        setupMoodClickListener(view, R.id.mood_neutral, "Calm");
        setupMoodClickListener(view, R.id.mood_sad, "Feeling Down");
        setupMoodClickListener(view, R.id.mood_anxious, "Anxious");
        setupMoodClickListener(view, R.id.mood_angry, "Angry");
        setupMoodClickListener(view, R.id.mood_tired, "Tired");
        setupMoodClickListener(view, R.id.mood_other, "Other");
    }

    private void setupMoodClickListener(View parent, int id, String mood) {
        View v = parent.findViewById(id);
        if (v != null) v.setOnClickListener(x -> saveMood(mood));
    }

    private void saveMood(String mood) {
        if (sharedPreferences == null) return;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        sharedPreferences.edit().putString(KEY_MOOD, mood).putString(KEY_DATE, date).putString(date, mood).apply();
        updateDisplay(mood);
        Toast.makeText(getContext(), "Mood saved!", Toast.LENGTH_SHORT).show();
    }

    private void checkAndDisplayMood() {
        if (sharedPreferences == null) return;
        String savedDate = sharedPreferences.getString(KEY_DATE, "");
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (date.equals(savedDate)) updateDisplay(sharedPreferences.getString(KEY_MOOD, ""));
        else if (tvRecentEntry != null) {
            tvRecentEntry.setText("No entries for today yet~");
            tvRecentEntry.setAlpha(0.6f);
        }
    }

    private void updateDisplay(String mood) {
        if (tvRecentEntry != null) {
            tvRecentEntry.setText("Today's mood is " + mood);
            tvRecentEntry.setAlpha(1.0f);
        }
    }
}