package com.example.stay_healthy;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "food_table")
public class Food {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String calories;
    public String mealType;
    public String date;

    public Food(String name, String calories, String mealType, String date) {
        this.name = name;
        this.calories = calories;
        this.mealType = mealType;
        this.date = date;
    }
}
