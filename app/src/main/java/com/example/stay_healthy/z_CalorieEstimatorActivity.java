package com.example.stay_healthy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class z_CalorieEstimatorActivity extends AppCompatActivity {
    private EditText foodInput;
    private Button estimateBtn;
    private TextView resultText;
    private Map<String, Integer> db = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calorie_estimator);

        foodInput = findViewById(R.id.foodInput);
        estimateBtn = findViewById(R.id.estimateBtn);
        resultText = findViewById(R.id.resultText);

        // simple static database
        db.put("apple", 95);
        db.put("banana", 105);
        db.put("egg", 78);
        db.put("bread", 79);
        db.put("rice", 206);

        estimateBtn.setOnClickListener(v -> {
            String f = foodInput.getText().toString().trim().toLowerCase();
            if(f.isEmpty()) {
                resultText.setText("Please enter food name");
                return;
            }
            Integer val = db.get(f);
            if(val == null) resultText.setText("Estimate not available");
            else resultText.setText(f + " ~ " + val + " kcal (per typical serving)"); 
        });
    }
}
