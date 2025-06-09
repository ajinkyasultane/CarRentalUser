package com.example.carrentaluser;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.carrentaluser.models.BankAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.Timestamp;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class AddBankAccountActivity extends AppCompatActivity {

    private EditText etAccountHolderName;
    private EditText etAccountNumber;
    private EditText etConfirmAccountNumber;
    private Spinner spinnerAccountType;
    private EditText etBankName;
    private EditText etIfscCode;
    private EditText etUpiId;
    private CheckBox cbSetAsPrimary;
    private Button btnSaveAccount;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bank_account);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add Bank Account");
        }
        
        // Initialize views
        etAccountHolderName = findViewById(R.id.etAccountHolderName);
        etAccountNumber = findViewById(R.id.etAccountNumber);
        etConfirmAccountNumber = findViewById(R.id.etConfirmAccountNumber);
        spinnerAccountType = findViewById(R.id.spinnerAccountType);
        etBankName = findViewById(R.id.etBankName);
        etIfscCode = findViewById(R.id.etIfscCode);
        etUpiId = findViewById(R.id.etUpiId);
        cbSetAsPrimary = findViewById(R.id.cbSetAsPrimary);
        btnSaveAccount = findViewById(R.id.btnSaveAccount);
        progressBar = findViewById(R.id.progressBar);
    
        // Hide the primary checkbox since all accounts are primary in single account mode
        cbSetAsPrimary.setVisibility(View.GONE);

        // Set up account type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.account_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccountType.setAdapter(adapter);

        // Set up save button click listener
        btnSaveAccount.setOnClickListener(v -> validateAndSaveAccount());
    }
    
    private void validateAndSaveAccount() {
        // Get input values
        String accountHolderName = etAccountHolderName.getText().toString().trim();
        String accountNumber = etAccountNumber.getText().toString().trim();
        String confirmAccountNumber = etConfirmAccountNumber.getText().toString().trim();
        String accountType = spinnerAccountType.getSelectedItem().toString();
        String bankName = etBankName.getText().toString().trim();
        String ifscCode = etIfscCode.getText().toString().trim();
        String upiId = etUpiId.getText().toString().trim();
        
        // In single account mode, all accounts are primary
        boolean isPrimary = true;
        
        // Hide the primary checkbox since it's always primary
        cbSetAsPrimary.setVisibility(View.GONE);
        
        // Enhanced validations
        
        // Account holder name validation
        if (TextUtils.isEmpty(accountHolderName)) {
            etAccountHolderName.setError("Account holder name is required");
            etAccountHolderName.requestFocus();
            return;
        }
        
        if (accountHolderName.length() < 3) {
            etAccountHolderName.setError("Account holder name must be at least 3 characters");
            etAccountHolderName.requestFocus();
            return;
        }
        
        if (!accountHolderName.matches("^[a-zA-Z\\s.]+$")) {
            etAccountHolderName.setError("Account holder name should contain only letters, spaces, and periods");
            etAccountHolderName.requestFocus();
            return;
        }
        
        // Account number validation
        if (TextUtils.isEmpty(accountNumber)) {
            etAccountNumber.setError("Account number is required");
            etAccountNumber.requestFocus();
            return;
        }
        
        if (!accountNumber.matches("^[0-9]{9,18}$")) {
            etAccountNumber.setError("Account number must be 9-18 digits");
            etAccountNumber.requestFocus();
            return;
        }
        
        if (!accountNumber.equals(confirmAccountNumber)) {
            etConfirmAccountNumber.setError("Account numbers do not match");
            etConfirmAccountNumber.requestFocus();
            return;
        }
        
        // Bank name validation
        if (TextUtils.isEmpty(bankName)) {
            etBankName.setError("Bank name is required");
            etBankName.requestFocus();
            return;
        }
        
        if (bankName.length() < 3) {
            etBankName.setError("Bank name must be at least 3 characters");
            etBankName.requestFocus();
            return;
        }
        
        // IFSC code validation
        if (TextUtils.isEmpty(ifscCode)) {
            etIfscCode.setError("IFSC code is required");
            etIfscCode.requestFocus();
            return;
        }
        
        if (!ifscCode.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            etIfscCode.setError("Invalid IFSC code format (e.g., SBIN0123456)");
            etIfscCode.requestFocus();
            return;
        }
        
        // UPI ID validation (optional)
        if (!TextUtils.isEmpty(upiId) && !upiId.matches("^[\\w.-]+@[\\w.-]+$")) {
            etUpiId.setError("Invalid UPI ID format (e.g., name@bank)");
            etUpiId.requestFocus();
            return;
        }
        
        // Show progress
        showLoading(true);

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            showLoading(false);
            Toast.makeText(this, "You must be logged in to add a bank account", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // First check if the user already has an account
        db.collection("users").document(userId).collection("bank_accounts")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    showLoading(false);
                    Toast.makeText(this, "You already have a bank account. Only one account is allowed.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                
                // Create new bank account object (always primary)
                BankAccount account = new BankAccount(accountHolderName, accountNumber, accountType, bankName, ifscCode, true, userId);
                
                // Set UPI ID if provided
                if (!TextUtils.isEmpty(upiId)) {
                    account.setUpiId(upiId);
                }
                
                // Save the account to Firestore
                db.collection("users").document(userId).collection("bank_accounts")
                    .add(account)
                    .addOnSuccessListener(documentReference -> {
                        showLoading(false);
                        Toast.makeText(AddBankAccountActivity.this, "Bank account added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e("AddBankAccount", "Error adding bank account", e);
                        Toast.makeText(AddBankAccountActivity.this, "Failed to add bank account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Log.e("AddBankAccount", "Error checking account count", e);
                Toast.makeText(AddBankAccountActivity.this, "Failed to check account limit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveAccount.setEnabled(!isLoading);
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