package com.example.stay_healthy;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button btnLogin;
    private TextView textForgotPassword;
    private TextView textRegister;
    private FirebaseAuth mAuth;
    private static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        editEmail = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        textRegister = findViewById(R.id.text_register);
        textForgotPassword = findViewById(R.id.text_forgot_password);

        btnLogin.setOnClickListener(v -> attemptLogin());
        textForgotPassword.setOnClickListener(v -> showPasswordRecoveryDialog());

        if (textRegister != null) {
            textRegister.setOnClickListener(v -> Register());
        }
    }
    private void Register(){
        startActivity(new Intent(LoginActivity.this,RegisterActivity.class));
    }
    private void attemptLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email and Password cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainPage.class));
                        finish();
                    } else {
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(LoginActivity.this, "Account not registered.", Toast.LENGTH_LONG).show();
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(LoginActivity.this, "Invalid password.", Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = exception != null ? exception.getMessage() : "Unknown error.";
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    private void showPasswordRecoveryDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter registered Email");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Password Reset")
                .setMessage("Enter your registered Email to receive a password reset link.")
                .setView(input)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String identifier = input.getText().toString().trim();
                    attemptPasswordRecovery(identifier);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void attemptPasswordRecovery(String identifier) {
        if (TextUtils.isEmpty(identifier) || !isEmailValid(identifier)) {
            Toast.makeText(this, "Account not registered.", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.sendPasswordResetEmail(identifier)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset link sent to " + identifier + ". Please check your email.",
                                Toast.LENGTH_LONG).show();

                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(LoginActivity.this, "Account not registered.", Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Error sending link.";
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    private boolean isEmailValid(String email) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainPage.class));
            finish();
        }
    }
}
