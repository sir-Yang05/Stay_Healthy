package com.example.stay_healthy;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// ✅ 修改点 1: entities 里面加了 Food.class
// ✅ 修改点 2: version 改成了 2 (因为结构变了)
@Database(entities = {Workout.class, Food.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WorkoutDao workoutDao();
    // ✅ 修改点 3: 新增 FoodDao
    public abstract FoodDao foodDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "workout_database")
                            .allowMainThreadQueries()
                            // ✅ 修改点 4: 允许破坏性升级 (旧数据会被清空，防止闪退)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}