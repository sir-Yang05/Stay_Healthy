package com.example.stay_healthy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private int dailyGoal = 500;

    private LinearLayout layoutRecentWorkouts;
    private TextView tvTotalCalories;
    private TextView tvTotalDuration;

    // 目标追踪
    private TextView tvGoalProgress;
    private ProgressBar progressLiveGoal;
    private View cardLiveGoal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button startButton = view.findViewById(R.id.btn_start_workout);
        layoutRecentWorkouts = view.findViewById(R.id.layout_recent_workouts);

        // 绑定 XML 里的 ID (现在 XML 里肯定有它们了)
        tvTotalCalories = view.findViewById(R.id.tv_total_calories);
        tvTotalDuration = view.findViewById(R.id.tv_total_duration);

        tvGoalProgress = view.findViewById(R.id.tv_goal_progress);
        progressLiveGoal = view.findViewById(R.id.progress_live_goal);
        cardLiveGoal = view.findViewById(R.id.card_live_tracking);

        // 设置日期
        TextView tvCalendarDate = view.findViewById(R.id.tv_calendar_date);
        if (tvCalendarDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            tvCalendarDate.setText(sdf.format(new Date()));
        }

        loadSavedGoal();

        if (cardLiveGoal != null) cardLiveGoal.setOnClickListener(v -> showEditGoalDialog());
        if (startButton != null) startButton.setOnClickListener(v -> showSportSelectionDialog());

        loadDataFromDatabase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataFromDatabase();
    }

    private void showSportSelectionDialog() {
        if (getContext() == null) return;
        final String[] sports = {"Running", "Walking", "Cycling", "Basketball", "Badminton"};
        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Choose Activity")
                .setItems(sports, (dialog, which) -> {
                    Intent intent = new Intent(getActivity(), RunningActivity.class);
                    intent.putExtra("SPORT_TYPE", sports[which]);
                    startActivity(intent);
                })
                .show();
    }

    private void loadSavedGoal() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("KeepHealthyPrefs", Context.MODE_PRIVATE);
        dailyGoal = prefs.getInt("user_goal", 500);
        if (progressLiveGoal != null) progressLiveGoal.setMax(dailyGoal);
    }

    private void showEditGoalDialog() {
        if (getContext() == null) return;
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter daily goal");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC0FF00));

        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Set Daily Goal")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = input.getText().toString();
                    if (!text.isEmpty()) {
                        dailyGoal = Integer.parseInt(text);
                        SharedPreferences prefs = getActivity().getSharedPreferences("KeepHealthyPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putInt("user_goal", dailyGoal).apply();
                        if (progressLiveGoal != null) progressLiveGoal.setMax(dailyGoal);
                        loadDataFromDatabase();
                        Toast.makeText(getContext(), "Goal Updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadDataFromDatabase() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(requireContext());
        List<Workout> workoutList = db.workoutDao().getAllWorkouts();

        if (layoutRecentWorkouts != null) layoutRecentWorkouts.removeAllViews();
        else return;

        int totalKcal = 0;
        int totalMinutes = 0;

        for (Workout workout : workoutList) {
            if (workout.calories != null) {
                try {
                    totalKcal += Integer.parseInt(workout.calories.replace(" kcal", "").trim());
                } catch (Exception e) {}
            }
            if (workout.duration != null) {
                try {
                    String durStr = workout.duration.toLowerCase();
                    if (durStr.contains("mins") || durStr.contains("min")) {
                        totalMinutes += Integer.parseInt(durStr.replace(" mins", "").replace(" min", "").trim());
                    }
                } catch (Exception e) {}
            }

            View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_workout, layoutRecentWorkouts, false);
            TextView tvType = cardView.findViewById(R.id.tv_item_type);
            TextView tvDate = cardView.findViewById(R.id.tv_item_date);
            TextView tvDuration = cardView.findViewById(R.id.tv_item_duration);
            TextView tvCalories = cardView.findViewById(R.id.tv_item_calories);
            ImageView imgIcon = cardView.findViewById(R.id.img_item_icon);
            ImageView btnDelete = cardView.findViewById(R.id.btn_delete_item);

            tvType.setText(workout.type);
            tvDate.setText(workout.date);
            tvDuration.setText(workout.duration);
            tvCalories.setText(workout.calories);

            tvType.setTextColor(Color.WHITE);
            tvDate.setTextColor(Color.LTGRAY);
            tvDuration.setTextColor(Color.WHITE);
            tvCalories.setTextColor(Color.WHITE);

            if (imgIcon != null) {
                switch (workout.type) {
                    case "Walking": imgIcon.setImageResource(R.drawable.ic_walking); imgIcon.setColorFilter(0xFFFFA726); break;
                    case "Cycling": imgIcon.setImageResource(R.drawable.ic_cycling); imgIcon.setColorFilter(0xFF29B6F6); break;
                    case "Basketball": imgIcon.setImageResource(R.drawable.ic_basketball); imgIcon.setColorFilter(0xFFFF5722); break;
                    case "Badminton": imgIcon.setImageResource(R.drawable.ic_badminton); imgIcon.setColorFilter(0xFFAB47BC); break;
                    default: imgIcon.setImageResource(R.drawable.ic_running); imgIcon.setColorFilter(0xFFC0FF00); break;
                }
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    db.workoutDao().delete(workout);
                    loadDataFromDatabase();
                });
            }
            layoutRecentWorkouts.addView(cardView);
        }

        if (tvTotalCalories != null) tvTotalCalories.setText(totalKcal + " kcal");
        if (tvTotalDuration != null) tvTotalDuration.setText(totalMinutes + " mins");

        if (progressLiveGoal != null && tvGoalProgress != null) {
            progressLiveGoal.setMax(dailyGoal);
            progressLiveGoal.setProgress(totalKcal);
            tvGoalProgress.setText(totalKcal + " / " + dailyGoal + " kcal");
            if (totalKcal >= dailyGoal) {
                tvGoalProgress.setTextColor(0xFFC0FF00);
                tvGoalProgress.setText("Goal Reached!");
            } else {
                tvGoalProgress.setTextColor(Color.WHITE);
            }
        }
    }
}