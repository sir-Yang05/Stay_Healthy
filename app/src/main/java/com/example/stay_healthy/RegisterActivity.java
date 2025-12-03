package com.example.stay_healthy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button registerBtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        registerBtn = findViewById(R.id.registerBtn);

        registerBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            if(email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        startActivity(new Intent(RegisterActivity.this, MainPage.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Register failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });
    }
}
