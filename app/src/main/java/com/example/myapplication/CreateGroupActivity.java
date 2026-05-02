package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText editTextGroupName;
    private EditText editTextGroupPurpose;
    private Button buttonCreateGroup;
    private TextView textViewGroupStatus;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        editTextGroupName = findViewById(R.id.editTextGroupName);
        editTextGroupPurpose = findViewById(R.id.editTextGroupPurpose);
        buttonCreateGroup = findViewById(R.id.buttonCreateGroup);
        textViewGroupStatus = findViewById(R.id.textViewGroupStatus);
        ImageView imageViewBack = findViewById(R.id.imageViewBack);

        imageViewBack.setOnClickListener(v -> finish());
        buttonCreateGroup.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        String groupName = editTextGroupName.getText().toString().trim();
        String groupPurpose = editTextGroupPurpose.getText().toString().trim();

        textViewGroupStatus.setVisibility(View.GONE);

        if (TextUtils.isEmpty(groupName)) {
            editTextGroupName.setError("Enter group name");
            editTextGroupName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(groupPurpose)) {
            editTextGroupPurpose.setError("Enter group purpose");
            editTextGroupPurpose.requestFocus();
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            showError("User session expired. Please log in again.");
            return;
        }

        buttonCreateGroup.setEnabled(false);
        buttonCreateGroup.setText("Creating group...");

        Map<String, Object> groupMap = new HashMap<>();
        groupMap.put("groupName", groupName);
        groupMap.put("groupPurpose", groupPurpose);
        groupMap.put("createdBy", currentUser.getUid());
        groupMap.put("creatorEmail", currentUser.getEmail());
        groupMap.put("createdAt", System.currentTimeMillis());

        firestore.collection("groups")
                .add(groupMap)
                .addOnSuccessListener(documentReference -> {
                    buttonCreateGroup.setEnabled(true);
                    buttonCreateGroup.setText("Create Group");
                    Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonCreateGroup.setEnabled(true);
                    buttonCreateGroup.setText("Create Group");
                    showError("Failed to create group: " + e.getMessage());
                });
    }

    private void showError(String message) {
        textViewGroupStatus.setText(message);
        textViewGroupStatus.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
