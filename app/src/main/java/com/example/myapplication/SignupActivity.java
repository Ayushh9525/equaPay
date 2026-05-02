package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private Button buttonSignup;
    private TextView textViewSignupStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        editTextName = findViewById(R.id.editTextSignupName);
        editTextEmail = findViewById(R.id.editTextSignupEmail);
        editTextPassword = findViewById(R.id.editTextSignupPassword);
        buttonSignup = findViewById(R.id.buttonSignup);
        textViewSignupStatus = findViewById(R.id.textViewSignupStatus);
        TextView textViewGoToLogin = findViewById(R.id.textViewGoToLogin);

        buttonSignup.setOnClickListener(v -> createAccount());

        textViewGoToLogin.setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        textViewSignupStatus.setVisibility(View.GONE);

        if (name.isEmpty()) {
            editTextName.setError("Enter your name");
            editTextName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            editTextEmail.setError("Enter your email");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Enter your password");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        buttonSignup.setEnabled(false);
        buttonSignup.setText("Creating account...");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        String userId = firebaseAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, name, email);
                    } else {
                        buttonSignup.setEnabled(true);
                        buttonSignup.setText("Sign Up");
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Signup failed";
                        textViewSignupStatus.setText(errorMessage);
                        textViewSignupStatus.setVisibility(View.VISIBLE);
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("createdAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(userId)
                .set(userMap)
                .addOnSuccessListener(unused -> {
                    buttonSignup.setEnabled(true);
                    buttonSignup.setText("Sign Up");
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonSignup.setEnabled(true);
                    buttonSignup.setText("Sign Up");
                    String errorMessage = "User created, but profile save failed: " + e.getMessage();
                    textViewSignupStatus.setText(errorMessage);
                    textViewSignupStatus.setVisibility(View.VISIBLE);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                });
    }
}
