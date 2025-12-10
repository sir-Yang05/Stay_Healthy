package com.example.stay_healthy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private ImageView profileImage, settingsIcon;
    private TextView emailText, signatureText, genderText, birthdayText;

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private ValueEventListener userListener;

    private static final String DB_URL = "https://stay-healthy-6d8ff-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.activity_profile, container, false);

        profileImage = v.findViewById(R.id.profile_image);
        settingsIcon = v.findViewById(R.id.settings_icon);
        emailText = v.findViewById(R.id.email_display_text);
        signatureText = v.findViewById(R.id.signature_display_text);
        genderText = v.findViewById(R.id.gender_display_text);
        birthdayText = v.findViewById(R.id.birthday_display_text);

        auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return v;

        emailText.setText(firebaseUser.getEmail());

        userRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users")
                .child(firebaseUser.getUid());

        settingsIcon.setOnClickListener(this::showPopupMenu);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        listenUserData();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenuInflater().inflate(R.menu.profile_settings_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_profile) {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.action_log_out) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes", (d, i) -> {
                            auth.signOut();
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void listenUserData() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null || getContext() == null) return;

                signatureText.setText(user.signature);
                genderText.setText(user.gender);
                birthdayText.setText(user.birthday);

                if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                    byte[] decodedString = Base64.decode(user.profileImageUrl, Base64.DEFAULT);
                    Glide.with(getContext()).load(decodedString).into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.circle);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        userRef.addValueEventListener(userListener);
    }
}
