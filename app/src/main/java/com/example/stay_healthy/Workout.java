package com.example.stay_healthy;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_table")
public class Workout {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type;
    public String date;
    public String time;
    public String duration;
    public String distance;
    public String calories;
    public String pace;

    public Workout() {
    }

    @Ignore
    public Workout(String date, String duration, String calories, String distance) {
        this.date = date;
        this.duration = duration;
        this.calories = calories;
        this.distance = distance;
    }
}