package com.example.stay_healthy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MentalWellnessPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mental_wellness_page);

        Button ExerciseButton = findViewById(R.id.exercise);
        Button dietButton = findViewById(R.id.mental);
        Button wellnessButton = findViewById(R.id.dietary);
        Button profileButton = findViewById(R.id.profile);

        ExerciseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MentalWellnessPage.this, MainPage.class));
            }
        });

        dietButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MentalWellnessPage.this, DietoryPage.class));
            }
        });

        wellnessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MentalWellnessPage.this, MentalWellnessPage.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
        });

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MentalWellnessPage.this, Profile.class));
            }
        });
    }
}