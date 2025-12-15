package com.example.stay_healthy;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FoodDao {
    @Insert
    void insert(Food food);

    @Query("SELECT * FROM food_table WHERE date = :date")
    List<Food> getFoodsByDate(String date);

    @Delete
    void delete(Food food);
}
