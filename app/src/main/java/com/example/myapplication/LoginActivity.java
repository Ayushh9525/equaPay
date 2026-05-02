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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private Button buttonLogin;
    private TextView textViewLoginStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        editTextEmail = findViewById(R.id.editTextLoginEmail);
        editTextPassword = findViewById(R.id.editTextLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewLoginStatus = findViewById(R.id.textViewLoginStatus);
        TextView textViewGoToSignup = findViewById(R.id.textViewGoToSignup);

        buttonLogin.setOnClickListener(v -> loginUser());

        textViewGoToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        textViewLoginStatus.setVisibility(View.GONE);

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

        buttonLogin.setEnabled(false);
        buttonLogin.setText("Logging in...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    buttonLogin.setEnabled(true);
                    buttonLogin.setText("Login");

                    if (task.isSuccessful()) {
                        ensureUsernameAndOpenMain();
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        textViewLoginStatus.setText(errorMessage);
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (firebaseAuth.getCurrentUser() != null) {
            ensureUsernameAndOpenMain();
        }
    }

    private void ensureUsernameAndOpenMain() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String existingUsername = documentSnapshot.getString("username");

                    if (existingUsername != null && !existingUsername.trim().isEmpty()) {
                        openMainScreen();
                    } else {
                        String generatedUsername = generateUsername(currentUser);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("uid", currentUser.getUid());
                        updates.put("email", currentUser.getEmail());
                        if (!documentSnapshot.contains("name")) {
                            updates.put("name", currentUser.getEmail() != null ? currentUser.getEmail() : "User");
                        }
                        updates.put("username", generatedUsername);

                        firestore.collection("users")
                                .document(currentUser.getUid())
                                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Username created: " + generatedUsername, Toast.LENGTH_LONG).show();
                                    openMainScreen();
                                })
                                .addOnFailureListener(e -> {
                                    textViewLoginStatus.setText("Login worked, but username setup failed: " + e.getMessage());
                                    textViewLoginStatus.setVisibility(View.VISIBLE);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    textViewLoginStatus.setText("Failed to load profile: " + e.getMessage());
                    textViewLoginStatus.setVisibility(View.VISIBLE);
                });
    }

    private String generateUsername(FirebaseUser currentUser) {
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "user";
        String prefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        prefix = prefix.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase(Locale.US);

        if (prefix.length() < 4) {
            prefix = prefix + "user";
        }

        String uid = currentUser.getUid();
        String suffix = uid.length() >= 4 ? uid.substring(0, 4).toLowerCase(Locale.US) : "0001";
        return prefix + "_" + suffix;
    }

    private void openMainScreen() {
        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
