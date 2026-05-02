package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView textViewProfileName;
    private TextView textViewProfileEmail;
    private TextView textViewProfileUsername;
    private FirebaseFirestore firestore;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firestore = FirebaseFirestore.getInstance();
        textViewProfileName = view.findViewById(R.id.textViewProfileName);
        textViewProfileEmail = view.findViewById(R.id.textViewProfileEmail);
        textViewProfileUsername = view.findViewById(R.id.textViewProfileUsername);
        TextView textViewEditProfile = view.findViewById(R.id.textViewEditProfile);
        TextView textViewLogout = view.findViewById(R.id.textViewLogout);
        textViewEditProfile.setOnClickListener(v -> openEditProfile());
        textViewLogout.setOnClickListener(v -> logoutUser());

        loadProfile();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || getActivity() == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String username = documentSnapshot.getString("username");

                    textViewProfileName.setText(name != null ? name : "User");
                    textViewProfileEmail.setText(email != null ? email : "No email");
                    textViewProfileUsername.setText("Username: " + (username != null ? username : "not set"));
                })
                .addOnFailureListener(e -> {
                    textViewProfileUsername.setText("Username: unavailable");
                    Toast.makeText(getActivity(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void openEditProfile() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        if (getActivity() != null) {
            Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
