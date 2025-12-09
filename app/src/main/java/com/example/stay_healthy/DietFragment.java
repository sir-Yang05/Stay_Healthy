package com.example.stay_healthy;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DietFragment extends Fragment {

    // UI 控件
    private TextView tvCalEaten, tvCalGoalLabel;
    private ProgressBar progressCal;
    private TextView tvBreakfastSummary, tvLunchSummary, tvDinnerSummary;
    private TextView tvWaterCount, tvWaterRec;
    private View cardWater;
    private ImageView btnClearAll; // 清空按钮

    // 容器 (用于点击管理)
    private View rowBreakfast, rowLunch, rowDinner;

    // 基础目标
    private static final int BASE_CALORIE_GOAL = 1800;
    private static final int BASE_WATER_GOAL = 2000;
    private int currentWaterMl = 0;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private EditText tempEtName, tempEtCal;
    private ImageView tempImgPreview;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == -1 && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (tempImgPreview != null) {
                            tempImgPreview.setImageBitmap(imageBitmap);
                            tempImgPreview.setVisibility(View.VISIBLE);
                        }
                        simulateAIAnalysis();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diet, container, false);

        // 1. 绑定控件
        tvCalEaten = view.findViewById(R.id.tv_cal_eaten);
        tvCalGoalLabel = view.findViewById(R.id.tv_cal_goal_label);
        progressCal = view.findViewById(R.id.progress_calories);
        btnClearAll = view.findViewById(R.id.btn_clear_all_diet);

        tvBreakfastSummary = view.findViewById(R.id.tv_breakfast_summary);
        tvLunchSummary = view.findViewById(R.id.tv_lunch_summary);
        tvDinnerSummary = view.findViewById(R.id.tv_dinner_summary);

        rowBreakfast = view.findViewById(R.id.row_breakfast);
        rowLunch = view.findViewById(R.id.row_lunch);
        rowDinner = view.findViewById(R.id.row_dinner);

        tvWaterCount = view.findViewById(R.id.tv_water_count);
        tvWaterRec = view.findViewById(R.id.tv_water_recommendation);
        cardWater = view.findViewById(R.id.card_water);

        ImageView btnAddBreakfast = view.findViewById(R.id.btn_add_breakfast);
        ImageView btnAddLunch = view.findViewById(R.id.btn_add_lunch);
        ImageView btnAddDinner = view.findViewById(R.id.btn_add_dinner);

        // 2. 设置点击事件 (加号)
        btnAddBreakfast.setOnClickListener(v -> showAddFoodDialog("Breakfast"));
        btnAddLunch.setOnClickListener(v -> showAddFoodDialog("Lunch"));
        if (btnAddDinner != null) btnAddDinner.setOnClickListener(v -> showAddFoodDialog("Dinner"));

        // 3. 设置点击事件 (管理/删除单项)
        // 点击整行 -> 弹出管理列表
        rowBreakfast.setOnClickListener(v -> showManageFoodDialog("Breakfast"));
        rowLunch.setOnClickListener(v -> showManageFoodDialog("Lunch"));
        if (rowDinner != null) rowDinner.setOnClickListener(v -> showManageFoodDialog("Dinner"));

        // 4. 设置点击事件 (清空今日)
        btnClearAll.setOnClickListener(v -> showClearAllDialog());

        // 5. 设置水点击
        cardWater.setOnClickListener(v -> showAddWaterDialog());

        loadData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    // === 核心逻辑：加载数据 ===
    private void loadData() {
        if (getContext() == null) return;

        SharedPreferences prefs = getActivity().getSharedPreferences("KeepHealthyPrefs", Context.MODE_PRIVATE);
        currentWaterMl = prefs.getInt("water_ml", 0);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        int exerciseCalories = 0;
        try {
            List<Workout> workouts = db.workoutDao().getAllWorkouts();
            for (Workout w : workouts) {
                if (w.calories != null) {
                    exerciseCalories += Integer.parseInt(w.calories.replace(" kcal", "").trim());
                }
            }
        } catch (Exception e) {}

        int dynamicCalGoal = BASE_CALORIE_GOAL + exerciseCalories;
        int dynamicWaterGoal = BASE_WATER_GOAL + exerciseCalories;

        List<Food> foods = db.foodDao().getFoodsByDate(todayDate);
        int totalEaten = 0;
        StringBuilder breakfastText = new StringBuilder();
        StringBuilder lunchText = new StringBuilder();
        StringBuilder dinnerText = new StringBuilder();

        for (Food food : foods) {
            int cal = 0;
            try { cal = Integer.parseInt(food.calories); } catch (Exception e) {}
            totalEaten += cal;

            if ("Breakfast".equals(food.mealType)) breakfastText.append(food.name).append(" (").append(cal).append("), ");
            else if ("Lunch".equals(food.mealType)) lunchText.append(food.name).append(" (").append(cal).append("), ");
            else if ("Dinner".equals(food.mealType)) dinnerText.append(food.name).append(" (").append(cal).append("), ");
        }

        tvCalEaten.setText(String.valueOf(totalEaten));
        tvCalGoalLabel.setText("/ " + dynamicCalGoal + " kcal");
        progressCal.setMax(dynamicCalGoal);
        progressCal.setProgress(totalEaten);

        if (breakfastText.length() > 0) tvBreakfastSummary.setText(breakfastText.toString());
        else tvBreakfastSummary.setText("No food added");

        if (lunchText.length() > 0) tvLunchSummary.setText(lunchText.toString());
        else tvLunchSummary.setText("No food added");

        if (tvDinnerSummary != null) {
            if (dinnerText.length() > 0) tvDinnerSummary.setText(dinnerText.toString());
            else tvDinnerSummary.setText("No food added");
        }

        tvWaterCount.setText(currentWaterMl + " / " + dynamicWaterGoal + " ml");
        tvWaterRec.setText("Goal increased by " + exerciseCalories + "ml");
    }

    // === 功能 1：管理/删除单项食物 ===
    private void showManageFoodDialog(String mealType) {
        if (getContext() == null) return;

        AppDatabase db = AppDatabase.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        // 1. 获取当天的所有食物
        List<Food> allFoods = db.foodDao().getFoodsByDate(todayDate);

        // 2. 筛选出当前餐点的食物 (比如只看早餐)
        List<Food> mealFoods = new ArrayList<>();
        List<String> displayList = new ArrayList<>();

        for (Food f : allFoods) {
            if (mealType.equals(f.mealType)) {
                mealFoods.add(f);
                displayList.add(f.name + " (" + f.calories + " kcal)");
            }
        }

        if (displayList.isEmpty()) {
            Toast.makeText(getContext(), "No items to delete in " + mealType, Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 弹出列表对话框
        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Manage " + mealType)
                .setItems(displayList.toArray(new String[0]), (dialog, which) -> {
                    // 点击某一项，确认删除
                    Food foodToDelete = mealFoods.get(which);
                    confirmDeleteOne(foodToDelete);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmDeleteOne(Food food) {
        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Delete Item?")
                .setMessage("Delete " + food.name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.getInstance(requireContext()).foodDao().delete(food);
                    Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                    loadData(); // 刷新界面
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // === 功能 2：清空今日所有数据 ===
    private void showClearAllDialog() {
        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Reset Day?")
                .setMessage("Delete ALL food records for today?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    clearAllTodayData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllTodayData() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        List<Food> foods = db.foodDao().getFoodsByDate(todayDate);
        for (Food f : foods) {
            db.foodDao().delete(f);
        }

        Toast.makeText(getContext(), "Daily Log Cleared", Toast.LENGTH_SHORT).show();
        loadData();
    }

    // --- 以下是原有的添加、拍照等代码 (保持不变) ---

    private void showAddFoodDialog(String mealType) {
        if (getContext() == null) return;
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView btnCamera = new ImageView(getContext());
        btnCamera.setImageResource(R.drawable.ic_camera);
        btnCamera.setColorFilter(0xFFC0FF00);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
        params.bottomMargin = 20;
        btnCamera.setLayoutParams(params);
        btnCamera.setOnClickListener(v -> openCamera());
        layout.addView(btnCamera);

        TextView tvHint = new TextView(getContext());
        tvHint.setText("Tap camera to auto-detect");
        tvHint.setTextColor(Color.LTGRAY);
        tvHint.setTextSize(12);
        tvHint.setGravity(Gravity.CENTER);
        layout.addView(tvHint);

        tempImgPreview = new ImageView(getContext());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(200, 200);
        previewParams.topMargin = 20;
        previewParams.bottomMargin = 20;
        tempImgPreview.setLayoutParams(previewParams);
        tempImgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tempImgPreview.setVisibility(View.GONE);
        layout.addView(tempImgPreview);

        tempEtName = new EditText(getContext());
        tempEtName.setHint("Food Name");
        tempEtName.setTextColor(Color.WHITE);
        tempEtName.setHintTextColor(Color.LTGRAY);
        tempEtName.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC0FF00));

        tempEtCal = new EditText(getContext());
        tempEtCal.setHint("Calories");
        tempEtCal.setInputType(InputType.TYPE_CLASS_NUMBER);
        tempEtCal.setTextColor(Color.WHITE);
        tempEtCal.setHintTextColor(Color.LTGRAY);
        tempEtCal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC0FF00));

        layout.addView(tempEtName);
        layout.addView(tempEtCal);

        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Add to " + mealType)
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = tempEtName.getText().toString();
                    String cal = tempEtCal.getText().toString();
                    if (!name.isEmpty() && !cal.isEmpty()) {
                        saveFood(name, cal, mealType);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveFood(String name, String cal, String mealType) {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());
        Food newFood = new Food(name, cal, mealType, todayDate);
        db.foodDao().insert(newFood);
        Toast.makeText(getContext(), "Food Added!", Toast.LENGTH_SHORT).show();
        loadData();
    }

    private void showAddWaterDialog() {
        if (getContext() == null) return;
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Amount in ml (e.g. 200)");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));

        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Add Water")
                .setMessage("How much did you drink?")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = input.getText().toString();
                    if (!text.isEmpty()) {
                        int addMl = Integer.parseInt(text);
                        currentWaterMl += addMl;
                        SharedPreferences prefs = getActivity().getSharedPreferences("KeepHealthyPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putInt("water_ml", currentWaterMl).apply();
                        loadData();
                        Toast.makeText(getContext(), "Water Added!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void simulateAIAnalysis() {
        Toast.makeText(getContext(), "AI Analyzing...", Toast.LENGTH_SHORT).show();
        new android.os.Handler().postDelayed(() -> {
            String[] foods = {"Apple", "Banana", "Chicken Salad", "Steak", "Rice", "Burger"};
            int[] cals = {52, 89, 350, 450, 130, 550};
            int index = new Random().nextInt(foods.length);
            if (tempEtName != null) tempEtName.setText(foods[index]);
            if (tempEtCal != null) tempEtCal.setText(String.valueOf(cals[index]));
            Toast.makeText(getContext(), "Identified: " + foods[index], Toast.LENGTH_SHORT).show();
        }, 1000);
    }
}