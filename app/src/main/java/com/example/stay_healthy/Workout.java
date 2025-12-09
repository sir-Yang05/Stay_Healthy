package com.example.stay_healthy;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// @Entity 表示这就是一张表，表名叫 "workout_table"
@Entity(tableName = "workout_table")
public class Workout {

    // 每一条记录都需要一个独一无二的 ID，自动生成
    @PrimaryKey(autoGenerate = true)
    public int id;

    // 我们要记录的三个核心数据
    public String type;      // 运动类型 (Running, Cycling...)
    public String duration;  // 时长 (30 mins)
    public String calories;  // 卡路里 (300 kcal)

    // 还可以加一个日期
    public String date;      // 日期 (TODAY - 7:00 AM)

    // 构造函数
    public Workout(String type, String duration, String calories, String date) {
        this.type = type;
        this.duration = duration;
        this.calories = calories;
        this.date = date;
    }
}
