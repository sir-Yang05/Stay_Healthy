package com.example.stay_healthy;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_table")
public class Workout {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type;      // 运动类型 (例如 "Running", "Walking") <--- 新增
    public String date;      // 日期
    public String time;      // 时间
    public String duration;  // 时长
    public String distance;  // 距离
    public String calories;  // 卡路里
    public String pace;      // 配速

    // 无参构造函数
    public Workout() {
    }

    // 兼容旧代码
    @Ignore
    public Workout(String date, String duration, String calories, String distance) {
        this.date = date;
        this.duration = duration;
        this.calories = calories;
        this.distance = distance;
    }
}