package com.example.stay_healthy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Profile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        Button ExerciseButton = findViewById(R.id.exercise);
        Button dietButton = findViewById(R.id.mental);
        Button wellnessButton = findViewById(R.id.dietary);
        Button profileButton = findViewById(R.id.profile);

        ExerciseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, MainPage.class));
            }
        });

        dietButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, DietoryPage.class));
            }
        });

        wellnessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, MentalWellnessPage.class));
            }
        });

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 当前页面，使用 REORDER_TO_FRONT 避免重复创建
                startActivity(new Intent(Profile.this, Profile.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
        });
    }
}