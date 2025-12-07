package com.example.stay_healthy;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView changePhotoText;
    private RadioGroup genderRadioGroup;
    private TextView birthdayText;
    private EditText signatureEditText;
    private Button logoutButton, saveButton;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri tempImageUri;
    private String currentImageBase64 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://stay-healthy-ad450-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users");

        profileImage = findViewById(R.id.profile_image);
        changePhotoText = findViewById(R.id.change_photo_text);
        genderRadioGroup = findViewById(R.id.gender_radio_group);
        birthdayText = findViewById(R.id.birthday_text);
        signatureEditText = findViewById(R.id.signature_edittext);
        logoutButton = findViewById(R.id.logout_button);
        saveButton = findViewById(R.id.save);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        setupBottomNavigationView();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        tempImageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), tempImageUri);
                            profileImage.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        changePhotoText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        birthdayText.setOnClickListener(v -> showDatePickerDialog());
        saveButton.setOnClickListener(v -> saveProfileToDatabase());

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        loadUserProfile();
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                ProfileActivity.this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    birthdayText.setText(selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void saveProfileToDatabase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Saving...");
        pd.show();

        String signature = signatureEditText.getText().toString();
        String birthday = birthdayText.getText().toString();
        String gender = "";
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton rb = findViewById(selectedId);
            gender = rb.getText().toString();
        }

        String finalImageString;

        if (tempImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), tempImageUri);
                finalImageString = bitmapToString(bitmap);
            } catch (IOException e) {
                    pd.dismiss();
                return;
            }
        } else {
            finalImageString = currentImageBase64;
        }

        User userObj = new User(user.getEmail(), finalImageString, signature, gender, birthday);

        mDatabase.child(user.getUid()).setValue(userObj).addOnCompleteListener(task -> {
            pd.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(ProfileActivity.this, "Save Success!", Toast.LENGTH_SHORT).show();
                currentImageBase64 = finalImageString;
            } else {
                android.util.Log.e("ProfileSave", "Error: ", task.getException());
                Toast.makeText(ProfileActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 因为 User 类现在是独立文件，这里 User.class 就能正常识别了
            mDatabase.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;

                    User userProfile = snapshot.getValue(User.class);
                    if (userProfile != null) {
                        signatureEditText.setText(userProfile.signature);
                        birthdayText.setText(userProfile.birthday);
                        if (userProfile.gender != null) {
                            Log.d("GenderDebug", "The gender read from the database is: [" + userProfile.gender + "]");
                            String genderStr = userProfile.gender.trim();

                            if (userProfile.gender.equals("male")) {
                                genderRadioGroup.check(R.id.radio_male);
                            } else if (userProfile.gender.equals("female")) {
                                genderRadioGroup.check(R.id.radio_female);
                            } else if (userProfile.gender.equals("Do not show")){
                                genderRadioGroup.check(R.id.radio_none);
                            }
                        }

                        if (userProfile.profileImageUrl != null && !userProfile.profileImageUrl.isEmpty()) {
                            currentImageBase64 = userProfile.profileImageUrl;
                            Bitmap imageBitmap = stringToBitmap(userProfile.profileImageUrl);
                            if (imageBitmap != null) {
                                profileImage.setImageBitmap(imageBitmap);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    private String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        resized.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] imgBytes = baos.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    private Bitmap stringToBitmap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    private void setupBottomNavigationView() {

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.exercise) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.mental) {
                    startActivity(new Intent(getApplicationContext(), MentalWellnessPage.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.dietary) {
                    startActivity(new Intent(getApplicationContext(), DietoryPage.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.profile) {
                    return true;
                }
                return false;
            }
        });
    }
}
