package com.example.carrentaluser;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.example.carrentaluser.R;
import com.example.carrentaluser.utils.SessionManager;
import com.example.carrentaluser.utils.TokenManager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    private TextInputEditText emailInput, passwordInput;
    private Button loginBtn;
    private TextView goToRegister, forgotPasswordLink;
    private CheckBox rememberMeCheckbox;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);
        
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            // Redirect to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize UI elements
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        goToRegister = findViewById(R.id.goToRegister);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox);

        // Set up login button click listener
        loginBtn.setOnClickListener(v -> {
            loginUser();
        });

        // Set up register link click listener
        goToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
        
        // Set up forgot password link click listener
        forgotPasswordLink.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }
    
    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        
        // Show progress
        loginBtn.setEnabled(false);
        loginBtn.setText("Signing in...");
        
        // Authenticate with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Get current user
                    FirebaseUser user = mAuth.getCurrentUser();
                    
                    // Upload FCM token if needed
                    try {
                        TokenManager.uploadToken();
                    } catch (Exception e) {
                        Log.e(TAG, "Error uploading token: " + e.getMessage());
                    }
                    
                    // Save login session if Remember Me is checked
                    if (user != null) {
                        if (rememberMeCheckbox.isChecked()) {
                            // Create login session
                            sessionManager.createLoginSession(user.getUid(), user.getEmail());
                            Log.d(TAG, "Remember me enabled, saving session");
                        }
                    }

                    // Show success message
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to main activity
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Re-enable button
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Sign In");
                    
                    // Show error message
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
