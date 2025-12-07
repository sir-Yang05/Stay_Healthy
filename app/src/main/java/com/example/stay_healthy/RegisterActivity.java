package com.example.stay_healthy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput, phoneInput;
    private Button registerBtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.edit_username);
        phoneInput = findViewById(R.id.editTextPhone);
        passwordInput = findViewById(R.id.edit_password);
        registerBtn = findViewById(R.id.register);

        registerBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim(); //
            String pass = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(pass)) {
                Toast.makeText(this, "Please fill in all fields (Email, Phone, Password).", Toast.LENGTH_LONG).show();
                return;
            }
            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_LONG).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, MainPage.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Register failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}