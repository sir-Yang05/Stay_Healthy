package com.example.stay_healthy;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert
    void insert(Workout workout);

    @Query("SELECT * FROM workout_table ORDER BY id DESC")
    List<Workout> getAllWorkouts();

    @Delete
    void delete(Workout workout);
}