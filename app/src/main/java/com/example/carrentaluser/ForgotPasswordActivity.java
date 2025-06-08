package com.example.carrentaluser;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button resetPasswordBtn;
    private TextView backToLoginLink;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Forgot Password");
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        emailInput = findViewById(R.id.emailInputReset);
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn);
        backToLoginLink = findViewById(R.id.backToLoginLink);

        // Set up reset password button click listener
        resetPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        // Set up back to login link click listener
        backToLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        // Show progress
        findViewById(R.id.resetPasswordBtn).setEnabled(false);
        Toast.makeText(ForgotPasswordActivity.this, "Processing request...", Toast.LENGTH_SHORT).show();

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        findViewById(R.id.resetPasswordBtn).setEnabled(true);
                        
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, 
                                    "Password reset instructions sent to your email", 
                                    Toast.LENGTH_LONG).show();
                            
                            // Show success message and redirect after delay
                            new android.os.Handler().postDelayed(
                                    new Runnable() {
                                        public void run() {
                                            finish();
                                        }
                                    }, 2000);
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, 
                                    "Failed to send reset email. " + task.getException().getMessage(), 
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
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