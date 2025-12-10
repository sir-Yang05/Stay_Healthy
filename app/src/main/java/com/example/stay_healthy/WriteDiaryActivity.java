package com.example.stay_healthy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WriteDiaryActivity extends AppCompatActivity {

    private EditText diaryInput;
    private Button btnSave, btnCancel;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_diary);

        // 1. 绑定控件
        diaryInput = findViewById(R.id.diary_input);
        btnSave = findViewById(R.id.btn_save_diary);
        btnCancel = findViewById(R.id.btn_cancel);

        // 2. 检查用户登录状态并初始化数据库路径
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // [正常情况] 用户已登录 -> 存入 Users/UID/Diaries
            String uid = user.getUid();
            String dbUrl = "https://stay-healthy-6d8ff-default-rtdb.asia-southeast1.firebasedatabase.app/"; // 注意：一定要去网页复制准确的
            mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(uid).child("Diaries");
        } else {
            // [异常情况] 用户未登录 -> 提示并存入 DebugDiaries 防止闪退
            Toast.makeText(this, "Alert: Not logged in! Saving to Debug Mode.", Toast.LENGTH_LONG).show();
            mDatabase = FirebaseDatabase.getInstance().getReference("DebugDiaries");
        }

        // 3. 设置按钮监听器 (只需要写一次)
        btnSave.setOnClickListener(v -> saveToCloud());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveToCloud() {
        String content = diaryInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Empty content!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前时间对象
        Date now = new Date();

        // 生成时间格式 (注意：如果你想要中文日期，可以把 Locale.getDefault() 改成 Locale.CHINA)
        SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String fullDate = sdfFull.format(now);

        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        String year = sdfYear.format(now);

        SimpleDateFormat sdfMonth = new SimpleDateFormat("MM", Locale.getDefault());
        String month = sdfMonth.format(now);

        // 创建对象
        DiaryEntry entry = new DiaryEntry(content, fullDate, year, month);
        if (mDatabase == null) {
            android.util.Log.e("DEBUG_DIARY", "error：mDatabase is null！");
            Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show();
            return;
        }
        android.util.Log.d("DEBUG_DIARY", "Update to: " + mDatabase.toString());
        // 上传到 Firebase
        // 这里的 mDatabase 已经在 onCreate 里确定好是存哪了
        mDatabase.push().setValue(entry)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Saved to Cloud!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}