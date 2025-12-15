package com.example.stay_healthy;

public class DiaryEntry {
    public String key;
    public String content;
    public String fullDate;

    public String year;
    public String month;

    public DiaryEntry() {
    }

    public DiaryEntry(String content, String fullDate, String year, String month) {
        this.content = content;
        this.fullDate = fullDate;
        this.year = year;
        this.month = month;
    }
}