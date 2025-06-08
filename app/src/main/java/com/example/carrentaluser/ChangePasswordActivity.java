package com.example.carrentaluser;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";
    private EditText newPasswordInput;
    private EditText confirmPasswordInput;
    private Button changePasswordBtn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Change Password");
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        changePasswordBtn = findViewById(R.id.changePasswordBtn);

        // Verify user is logged in
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No authenticated user found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up change password button click listener
        changePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });
    }

    private void changePassword() {
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate password fields
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordInput.setError("New password is required");
            newPasswordInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Confirm password is required");
            confirmPasswordInput.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        // Check password strength
        if (!isStrongPassword(newPassword)) {
            newPasswordInput.setError("Password must be at least 8 characters, include numbers and special characters");
            newPasswordInput.requestFocus();
            return;
        }

        // Disable button to prevent multiple clicks
        changePasswordBtn.setEnabled(false);
        Toast.makeText(this, "Updating password...", Toast.LENGTH_SHORT).show();

        // Update password in Firebase
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            changePasswordBtn.setEnabled(true);
                            
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User password updated successfully");
                                Toast.makeText(ChangePasswordActivity.this,
                                        "Password updated successfully", Toast.LENGTH_SHORT).show();
                                
                                // Return to previous screen after success
                                new android.os.Handler().postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                finish();
                                            }
                                        }, 1500);
                            } else {
                                Log.e(TAG, "Error updating password", task.getException());
                                String errorMessage = "Failed to update password. ";
                                
                                if (task.getException() != null) {
                                    // Handle specific errors
                                    String exceptionMessage = task.getException().getMessage();
                                    if (exceptionMessage != null && exceptionMessage.contains("recent login")) {
                                        errorMessage += "Please log out and log in again before changing your password.";
                                    } else {
                                        errorMessage += exceptionMessage;
                                    }
                                }
                                
                                Toast.makeText(ChangePasswordActivity.this,
                                        errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    // Method to check if password is strong enough
    private boolean isStrongPassword(String password) {
        // Password must be at least 8 characters
        if (password.length() < 8) {
            return false;
        }

        // Check for at least one digit
        Pattern digitPattern = Pattern.compile("\\d");
        Matcher digitMatcher = digitPattern.matcher(password);
        if (!digitMatcher.find()) {
            return false;
        }

        // Check for at least one special character
        Pattern specialCharPattern = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
        Matcher specialCharMatcher = specialCharPattern.matcher(password);
        return specialCharMatcher.find();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 