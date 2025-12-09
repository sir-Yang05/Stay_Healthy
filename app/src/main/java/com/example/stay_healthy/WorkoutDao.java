package com.example.stay_healthy;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface WorkoutDao {

    // 1. 插入一条新记录 (存数据)
    @Insert
    void insert(Workout workout);

    // 2. 获取所有记录 (读数据)
    // ORDER BY id DESC 表示按 ID 倒序排列，这样最新的记录会显示在最上面
    @Query("SELECT * FROM workout_table ORDER BY id DESC")
    List<Workout> getAllWorkouts();

    // 3. 删除一条记录 (删数据)
    @Delete
    void delete(Workout workout);
}