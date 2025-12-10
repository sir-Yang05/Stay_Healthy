package com.example.stay_healthy;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private EditText signatureInput;
    private RadioGroup genderGroup;
    private RadioButton radioMale, radioFemale, radioSecret;
    private TextView birthdayText;
    private Button saveButton;

    private FirebaseUser firebaseUser;
    private DatabaseReference userRef;

    private Uri imageUri;
    private String newProfileImageBase64 = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private static final String DB_URL = "https://stay-healthy-6d8ff-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileImage = findViewById(R.id.profile_image);
        signatureInput = findViewById(R.id.signature_edittext);
        genderGroup = findViewById(R.id.gender_radio_group_edit);
        radioMale = findViewById(R.id.radio_male);
        radioFemale = findViewById(R.id.radio_female);
        radioSecret = findViewById(R.id.radio_none);
        birthdayText = findViewById(R.id.birthday_text);
        saveButton = findViewById(R.id.save);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(firebaseUser.getUid());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            profileImage.setImageBitmap(bitmap);
                            newProfileImageBase64 = bitmapToBase64(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        profileImage.setOnClickListener(v -> openImagePicker());
        birthdayText.setOnClickListener(v -> showDatePicker());
        saveButton.setOnClickListener(v -> saveData());

        loadUserInfo();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadUserInfo() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) return;

                signatureInput.setText(user.signature);
                birthdayText.setText(user.birthday);

                if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                    byte[] decodedString = Base64.decode(user.profileImageUrl, Base64.DEFAULT);
                    Glide.with(EditProfileActivity.this).load(decodedString).into(profileImage);
                }

                if ("Male".equals(user.gender)) radioMale.setChecked(true);
                else if ("Female".equals(user.gender)) radioFemale.setChecked(true);
                else radioSecret.setChecked(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, year, month, day) ->
                        birthdayText.setText(String.format("%04d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveData() {
        String signature = signatureInput.getText().toString().trim();
        String birthday = birthdayText.getText().toString();
        String gender;
        int id = genderGroup.getCheckedRadioButtonId();
        if (id == R.id.radio_male) gender = "Male";
        else if (id == R.id.radio_female) gender = "Female";
        else gender = "Secret";

        Map<String, Object> updates = new HashMap<>();
        updates.put("signature", signature);
        updates.put("gender", gender);
        updates.put("birthday", birthday);

        if (newProfileImageBase64 != null) {
            updates.put("profileImageUrl", newProfileImageBase64);
        }

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
