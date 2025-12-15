package com.example.stay_healthy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryDiaryActivity extends AppCompatActivity {

    private ListView listView;
    private Spinner spYear, spMonth;
    private TextView btnManage; // 右上角按钮
    private LinearLayout bottomActionBar; // 底部栏
    private Button btnCancel, btnDelete;

    private List<DiaryEntry> allEntries = new ArrayList<>(); // 所有数据
    private List<DiaryEntry> displayedEntries = new ArrayList<>(); // 当前显示的数据

    // 使用我们需要的新 Adapter
    private DiaryAdapter adapter;
    private DatabaseReference mDatabase;

    private String selectedYear = "All";
    private String selectedMonth = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historydiary);

        // 1. 初始化控件
        listView = findViewById(R.id.history_list_view);
        spYear = findViewById(R.id.spinner_year);
        spMonth = findViewById(R.id.spinner_month);
        btnManage = findViewById(R.id.btn_manage);
        bottomActionBar = findViewById(R.id.bottom_action_bar);
        btnCancel = findViewById(R.id.btn_action_cancel);
        btnDelete = findViewById(R.id.btn_action_delete);

        adapter = new DiaryAdapter(this, displayedEntries);
        listView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String dbUrl = "https://stay-healthy-6d8ff-default-rtdb.asia-southeast1.firebasedatabase.app/"; // 注意：一定要去网页复制准确的
            mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(uid).child("Diaries");
        } else {
            mDatabase = FirebaseDatabase.getInstance().getReference("DebugDiaries");
        }

        setupMonthSpinner();
        loadData();
        setupClickListeners();
    }

    private void setupClickListeners() {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            DiaryEntry entry = displayedEntries.get(position);

            if (adapter.isMultiSelectMode()) {
                adapter.toggleSelection(entry.key);
            } else {
                Intent intent = new Intent(HistoryDiaryActivity.this, DiaryDetailActivity.class);
                intent.putExtra("content", entry.content);
                intent.putExtra("date", entry.fullDate);
                startActivity(intent);
            }
        });

        btnManage.setOnClickListener(v -> {
            adapter.setMultiSelectMode(true);
            bottomActionBar.setVisibility(View.VISIBLE);
            btnManage.setVisibility(View.GONE);
            spYear.setEnabled(false);
            spMonth.setEnabled(false);
        });

        btnCancel.setOnClickListener(v -> exitMultiSelectMode());

        btnDelete.setOnClickListener(v -> {
            Set<String> keysToDelete = adapter.getSelectedKeys();
            if (keysToDelete.isEmpty()) {
                Toast.makeText(this, "Please select items first", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete Diaries")
                    .setMessage("Delete " + keysToDelete.size() + " items?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        for (String key : keysToDelete) {
                            if (key != null) {
                                mDatabase.child(key).removeValue();
                            }
                        }
                        Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                        exitMultiSelectMode(); // 删除完退出模式
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void exitMultiSelectMode() {
        adapter.setMultiSelectMode(false);
        bottomActionBar.setVisibility(View.GONE);
        btnManage.setVisibility(View.VISIBLE);
        spYear.setEnabled(true);
        spMonth.setEnabled(true);
    }

    private void setupMonthSpinner() {
        List<String> months = new ArrayList<>();
        months.add("All");
        for (int i = 1; i <= 12; i++) months.add(String.format("%02d", i));

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        spMonth.setAdapter(monthAdapter);

        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedMonth = months.get(pos);
                filterList();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void loadData() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEntries.clear();
                Set<String> yearSet = new HashSet<>();
                yearSet.add("All");

                for (DataSnapshot data : snapshot.getChildren()) {
                    DiaryEntry entry = data.getValue(DiaryEntry.class);
                    if (entry != null) {
                        entry.key = data.getKey();
                        allEntries.add(entry);
                        if (entry.year != null) yearSet.add(entry.year);
                    }
                }

                List<String> years = new ArrayList<>(yearSet);
                years.remove("All");
                Collections.sort(years, Collections.reverseOrder());
                years.add(0, "All");

                ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(HistoryDiaryActivity.this, android.R.layout.simple_spinner_dropdown_item, years);
                spYear.setAdapter(yearAdapter);

                spYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                        selectedYear = years.get(pos);
                        filterList();
                    }
                    @Override public void onNothingSelected(AdapterView<?> p) {}
                });

                filterList();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterList() {
        displayedEntries.clear();
        for (DiaryEntry entry : allEntries) {
            boolean yearMatch = selectedYear.equals("All") || (entry.year != null && entry.year.equals(selectedYear));
            boolean monthMatch = selectedMonth.equals("All") || (entry.month != null && entry.month.equals(selectedMonth));

            if (yearMatch && monthMatch) {
                displayedEntries.add(entry);
            }
        }
        Collections.reverse(displayedEntries);
        adapter.notifyDataSetChanged();
    }
}