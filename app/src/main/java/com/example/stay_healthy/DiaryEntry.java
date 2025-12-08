package com.example.stay_healthy;

public class DiaryEntry {
    public String key;       // Firebase 的唯一ID
    public String content;   // 日记内容
    public String fullDate;  // 完整时间，如 "2025-12-08 14:30" (用于显示)

    // [新增] 专门用于筛选的字段
    public String year;      // 如 "2025"
    public String month;     // 如 "12"

    public DiaryEntry() {
        // Firebase 需要空构造函数
    }

    public DiaryEntry(String content, String fullDate, String year, String month) {
        this.content = content;
        this.fullDate = fullDate;
        this.year = year;
        this.month = month;
    }
}