package com.example.stay_healthy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class z_AddJournalActivity extends AppCompatActivity {
    private EditText titleInput, bodyInput;
    private Button saveBtn;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_journal);

        titleInput = findViewById(R.id.titleInput);
        bodyInput = findViewById(R.id.bodyInput);
        saveBtn = findViewById(R.id.saveBtn);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        saveBtn.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String body = bodyInput.getText().toString().trim();
            FirebaseUser user = auth.getCurrentUser();
            if(user == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            if(title.isEmpty() && body.isEmpty()) {
                Toast.makeText(this, "Please enter title or body", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> doc = new HashMap<>();
            doc.put("title", title);
            doc.put("body", body);
            doc.put("timestamp", System.currentTimeMillis());
            db.collection("users").document(user.getUid()).collection("journals").add(doc)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
