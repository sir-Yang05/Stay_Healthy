package com.example.stay_healthy;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DiaryDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_detail);

        TextView tvDate = findViewById(R.id.tv_detail_date);
        TextView tvContent = findViewById(R.id.tv_detail_content);

        // 获取传过来的数据
        String date = getIntent().getStringExtra("date");
        String content = getIntent().getStringExtra("content");

        tvDate.setText(date);
        tvContent.setText(content);
    }
}