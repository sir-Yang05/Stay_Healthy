package com.example.stay_healthy;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
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
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DietFragment extends Fragment {


    private static final String GEMINI_API_KEY = "AIzaSyCG-vTBmCNeYtwbiXeJdbTannwllwLZDCk";

    // Gemini 1.5 Flash 接口地址
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY.trim();

    // 网络请求工具
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    // 界面控件
    private TextView tvCalEaten, tvCalGoalLabel;
    private ProgressBar progressCal;
    private TextView tvBreakfastSummary, tvLunchSummary, tvDinnerSummary;
    private TextView tvWaterCount, tvWaterRec;
    private View cardWater;
    private ImageView btnClearAll;

    // 数据变量
    private static final int BASE_CALORIE_GOAL = 1800;
    private static final int BASE_WATER_GOAL = 2000;
    private int currentWaterMl = 0;

    // 相机与弹窗相关
    private ActivityResultLauncher<Intent> cameraLauncher;
    private EditText tempEtName, tempEtCal;
    private ImageView tempImgPreview;
    private TextView tempTvAiHint;
    private View rowBreakfast, rowLunch, rowDinner;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化相机结果回调
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        // 如果弹窗还在，显示预览图
                        if (tempImgPreview != null) {
                            tempImgPreview.setImageBitmap(imageBitmap);
                            tempImgPreview.setVisibility(View.VISIBLE);
                        }
                        // 📸 开始 Gemini AI 识别
                        performGeminiAnalysis(imageBitmap);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diet, container, false);

        // 绑定控件
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

        // 设置点击事件
        btnAddBreakfast.setOnClickListener(v -> showAddFoodDialog("Breakfast"));
        btnAddLunch.setOnClickListener(v -> showAddFoodDialog("Lunch"));
        if (btnAddDinner != null) btnAddDinner.setOnClickListener(v -> showAddFoodDialog("Dinner"));

        // 点击整行管理/删除
        rowBreakfast.setOnClickListener(v -> showManageFoodDialog("Breakfast"));
        rowLunch.setOnClickListener(v -> showManageFoodDialog("Lunch"));
        if (rowDinner != null) rowDinner.setOnClickListener(v -> showManageFoodDialog("Dinner"));

        if (btnClearAll != null) btnClearAll.setOnClickListener(v -> showClearAllDialog());
        cardWater.setOnClickListener(v -> showAddWaterDialog());

        loadData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }



    // 1. 图片转 Base64 字符串
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    // 2. 发送请求给 Gemini
    private void performGeminiAnalysis(Bitmap imageBitmap) {
        if (GEMINI_API_KEY.contains("AIzaSy") == false) {
            // 简单的检查，如果你还没填Key
        }

        // 更新 UI 提示
        if (tempTvAiHint != null) {
            tempTvAiHint.setText("Gemini AI Analyzing... Please wait.");
            tempTvAiHint.setTextColor(0xFFC0FF00); // 荧光绿
        }
        if (tempEtName != null) tempEtName.setText("Thinking...");
        if (tempEtCal != null) tempEtCal.setText("");

        String base64Image = bitmapToBase64(imageBitmap);

        // 构建 Gemini 专用的 JSON 请求体
        // 我们要求它只返回 JSON 格式，不要废话
        String jsonBody = "{"
                + "\"contents\": [{"
                + "  \"parts\": ["
                + "    {\"text\": \"You are a nutritionist. Identify the food in this image and estimate its calories. Please return only one JSON object, formatted as follows：{\\\"food_name\\\": \\\"Food Name\\\", \\\"calories\\\": 0}。Do not use Markdown formatting. Do not add ```json tags. Return the JSON string directly.\"},"
                + "    {\"inline_data\": {"
                + "      \"mime_type\": \"image/jpeg\","
                + "      \"data\": \"" + base64Image + "\""
                + "    }}"
                + "  ]"
                + "}]"
                + "}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(API_URL).post(body).build();

        // 异步发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUi(() -> {
                    Toast.makeText(getContext(), "Network error. Please check your network connection.", Toast.LENGTH_SHORT).show();
                    if (tempTvAiHint != null) tempTvAiHint.setText("Connection failed");
                    if (tempEtName != null) tempEtName.setText("");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("GEMINI", responseBody); // 在 Logcat 里打印结果方便调试

                if (response.isSuccessful()) {
                    try {
                        // 1. 解析 Gemini 的外层结构
                        GeminiResponse geminiResp = gson.fromJson(responseBody, GeminiResponse.class);
                        String rawText = geminiResp.candidates.get(0).content.parts.get(0).text;

                        // 2. 清理可能存在的 markdown 符号 (以防万一)
                        rawText = rawText.replace("```json", "").replace("```", "").trim();

                        // 3. 解析我们需要的食物数据
                        AiFoodResult result = gson.fromJson(rawText, AiFoodResult.class);

                        // 4. 回到主线程更新 UI
                        runOnUi(() -> {
                            if (tempEtName != null) tempEtName.setText(result.foodName);
                            if (tempEtCal != null) tempEtCal.setText(String.valueOf(result.calories));
                            if (tempTvAiHint != null) {
                                tempTvAiHint.setText("Recognition complete!");
                                tempTvAiHint.setTextColor(Color.LTGRAY);
                            }
                            Toast.makeText(getContext(), "Recognition successful: " + result.foodName, Toast.LENGTH_SHORT).show();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUi(() -> {
                            Toast.makeText(getContext(), "Parsing failed. Please try again.", Toast.LENGTH_SHORT).show();
                            if (tempEtName != null) tempEtName.setText("Parsing error");
                        });
                    }
                } else {
                    runOnUi(() -> {
                        Toast.makeText(getContext(), "API Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        if (tempTvAiHint != null) tempTvAiHint.setText("Server Error");
                    });
                }
            }
        });
    }

    // 辅助方法：切换到主线程更新 UI
    private void runOnUi(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    // ==========================================
    // 常规逻辑 (数据加载、弹窗等)
    // ==========================================

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

        tempTvAiHint = new TextView(getContext());
        tempTvAiHint.setText("Tap the camera for AI recognition");
        tempTvAiHint.setTextColor(Color.LTGRAY);
        tempTvAiHint.setTextSize(12);
        tempTvAiHint.setGravity(Gravity.CENTER);
        layout.addView(tempTvAiHint);

        tempImgPreview = new ImageView(getContext());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(400, 400);
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

    private void saveFood(String name, String cal, String mealType) {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());
        Food newFood = new Food(name, cal, mealType, todayDate);
        db.foodDao().insert(newFood);
        Toast.makeText(getContext(), "Food Added!", Toast.LENGTH_SHORT).show();
        loadData();
    }

    private void showManageFoodDialog(String mealType) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());
        List<Food> allFoods = db.foodDao().getFoodsByDate(todayDate);
        List<Food> mealFoods = new ArrayList<>();
        List<String> displayList = new ArrayList<>();

        for (Food f : allFoods) {
            if (mealType.equals(f.mealType)) {
                mealFoods.add(f);
                displayList.add(f.name + " (" + f.calories + " kcal)");
            }
        }

        if (displayList.isEmpty()) {
            Toast.makeText(getContext(), "No items to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Manage " + mealType)
                .setItems(displayList.toArray(new String[0]), (dialog, which) -> {
                    Food foodToDelete = mealFoods.get(which);
                    db.foodDao().delete(foodToDelete);
                    loadData();
                    Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Reset Day?")
                .setMessage("Delete ALL food records for today?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String todayDate = sdf.format(new Date());
                    List<Food> foods = db.foodDao().getFoodsByDate(todayDate);
                    for (Food f : foods) db.foodDao().delete(f);
                    loadData();
                    Toast.makeText(getContext(), "Daily Log Cleared", Toast.LENGTH_SHORT).show();
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
}