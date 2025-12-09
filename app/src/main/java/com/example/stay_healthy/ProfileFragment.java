package com.example.stay_healthy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class ProfileFragment extends Fragment {

    // 控件变量
    private TextView birthdayText;
    private TextView changePhotoText;
    private ImageView profileImage;
    private EditText signatureEdit;
    private RadioGroup genderGroup;
    private RadioButton radioMale, radioFemale, radioSecret;

    // 存储用的文件名
    private static final String PREFS_NAME = "KeepHealthyPrefs";
    private static final String IMAGE_FILENAME = "profile_image.png"; // 头像保存的文件名

    // 启动器：相机 & 相册
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 初始化相机启动器
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        // 显示并保存
                        setAndSaveImage(imageBitmap);
                    }
                }
        );

        // 2. 初始化相册启动器
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        try {
                            // 将 Uri 转换为 Bitmap
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                            // 显示并保存
                            setAndSaveImage(imageBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);
        initViews(view);

        // 刚进来时，读取保存的数据
        loadProfileData();
        // 读取保存的头像
        loadSavedImage();

        return view;
    }

    private void initViews(View view) {
        birthdayText = view.findViewById(R.id.birthday_text);
        changePhotoText = view.findViewById(R.id.change_photo_text);
        profileImage = view.findViewById(R.id.profile_image);
        signatureEdit = view.findViewById(R.id.signature_edittext);
        genderGroup = view.findViewById(R.id.gender_radio_group);
        radioMale = view.findViewById(R.id.radio_male);
        radioFemale = view.findViewById(R.id.radio_female);
        radioSecret = view.findViewById(R.id.radio_none);

        Button btnLogout = view.findViewById(R.id.logout_button);
        Button btnSave = view.findViewById(R.id.save);

        if (birthdayText != null) {
            birthdayText.setOnClickListener(v -> showDatePicker());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfileData());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
        }

        // ✅ 头像点击：弹出选择框 (拍照 or 相册)
        View.OnClickListener photoListener = v -> showImagePicker();
        if (changePhotoText != null) changePhotoText.setOnClickListener(photoListener);
        if (profileImage != null) profileImage.setOnClickListener(photoListener);
    }

    // 🟢【新增】显示选择图片对话框
    private void showImagePicker() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        new AlertDialog.Builder(getContext(), R.style.DarkDialogTheme)
                .setTitle("Change Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 选项 0: 拍照
                        openCamera();
                    } else {
                        // 选项 1: 相册
                        openGallery();
                    }
                })
                .show();
    }

    // 打开相机
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    // 打开相册
    private void openGallery() {
        // 创建一个意图：选择内容，类型为图片
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // 🟢【新增】显示并保存图片到本地文件
    private void setAndSaveImage(Bitmap bitmap) {
        if (profileImage != null) {
            profileImage.setImageBitmap(bitmap);
        }

        // 保存到内部存储 (Internal Storage)
        try {
            // MODE_PRIVATE 表示只有本应用可以访问
            FileOutputStream fos = requireActivity().openFileOutput(IMAGE_FILENAME, Context.MODE_PRIVATE);
            // 压缩并写入 PNG 格式
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(getContext(), "Photo Updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving photo", Toast.LENGTH_SHORT).show();
        }
    }

    // 🟢【新增】启动时加载保存的图片
    private void loadSavedImage() {
        try {
            FileInputStream fis = requireActivity().openFileInput(IMAGE_FILENAME);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            if (profileImage != null) {
                profileImage.setImageBitmap(bitmap);
            }
            fis.close();
        } catch (Exception e) {
            // 如果文件不存在 (没设置过头像)，什么都不做，显示默认图
        }
    }

    private void saveProfileData() {
        if (getActivity() == null) return;
        String birthday = birthdayText.getText().toString();
        String signature = signatureEdit.getText().toString();

        String gender = "Secret";
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_male) gender = "Male";
        else if (selectedId == R.id.radio_female) gender = "Female";

        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("user_birthday", birthday);
        editor.putString("user_signature", signature);
        editor.putString("user_gender", gender);

        editor.apply();
        Toast.makeText(getContext(), "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
    }

    private void loadProfileData() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String savedBirthday = prefs.getString("user_birthday", "Please select birthday");
        if (birthdayText != null) birthdayText.setText(savedBirthday);

        String savedSignature = prefs.getString("user_signature", "");
        if (signatureEdit != null) signatureEdit.setText(savedSignature);

        String savedGender = prefs.getString("user_gender", "Secret");
        if (genderGroup != null) {
            if (savedGender.equals("Male") && radioMale != null) radioMale.setChecked(true);
            else if (savedGender.equals("Female") && radioFemale != null) radioFemale.setChecked(true);
            else if (radioSecret != null) radioSecret.setChecked(true);
        }
    }

    private void showDatePicker() {
        if (getContext() == null) return;
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dateString = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    birthdayText.setText(dateString);
                },
                year, month, day);

        Calendar minDate = Calendar.getInstance();
        minDate.set(1900, 0, 1);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        Calendar maxDate = Calendar.getInstance();
        maxDate.set(2099, 11, 31);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }
}