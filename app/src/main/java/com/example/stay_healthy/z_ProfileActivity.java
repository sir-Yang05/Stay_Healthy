package com.example.stay_healthy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class z_ProfileActivity extends AppCompatActivity {
    private EditText nameInput, mottoInput;
    private Button saveBtn, logoutBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameInput = findViewById(R.id.nameInput);
        mottoInput = findViewById(R.id.mottoInput);
        saveBtn = findViewById(R.id.saveBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if(user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // load existing profile
        DocumentReference ref = db.collection("users").document(user.getUid());
        ref.get().addOnSuccessListener(doc -> {
            if(doc.exists()) {
                nameInput.setText(doc.getString("name"));
                mottoInput.setText(doc.getString("motto"));
            }
        });

        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String motto = mottoInput.getText().toString().trim();
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("motto", motto);
            ref.set(data).addOnSuccessListener(a -> Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show())
               .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
