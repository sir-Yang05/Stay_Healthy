package com.example.stay_healthy;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MentalWellnessPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mental_wellness_page);

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
                    startActivity(new Intent(MentalWellnessPage.this, MentalWellnessPage.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    return true;
                } else if (itemId == R.id.profile) {
                    startActivity(new Intent(MentalWellnessPage.this, Profile.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                }
                return false;
            }
        });
    }
}
