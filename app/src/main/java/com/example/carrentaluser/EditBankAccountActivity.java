package com.example.carrentaluser;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditBankAccountActivity extends AppCompatActivity {

    private EditText etAccountHolderName;
    private EditText etAccountNumber;
    private Spinner spinnerAccountType;
    private EditText etBankName;
    private EditText etIfscCode;
    private EditText etUpiId;
    private CheckBox cbSetAsPrimary;
    private Button btnUpdateAccount;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String accountId;
    private BankAccount currentAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bank_account);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Edit Bank Account");
        }

        // Get account ID from intent
        accountId = getIntent().getStringExtra("account_id");
        if (accountId == null) {
            Toast.makeText(this, "Error: No account ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        etAccountHolderName = findViewById(R.id.etAccountHolderName);
        etAccountNumber = findViewById(R.id.etAccountNumber);
        spinnerAccountType = findViewById(R.id.spinnerAccountType);
        etBankName = findViewById(R.id.etBankName);
        etIfscCode = findViewById(R.id.etIfscCode);
        etUpiId = findViewById(R.id.etUpiId);
        cbSetAsPrimary = findViewById(R.id.cbSetAsPrimary);
        btnUpdateAccount = findViewById(R.id.btnUpdateAccount);
        progressBar = findViewById(R.id.progressBar);
        
        // Hide the primary checkbox since we only have one account that is always primary
        cbSetAsPrimary.setVisibility(View.GONE);

        // Set up account type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.account_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccountType.setAdapter(adapter);

        // Load account data
        loadAccountData();

        // Set up update button click listener
        btnUpdateAccount.setOnClickListener(v -> validateAndUpdateAccount());
    }

    private void loadAccountData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to edit an account", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);

        db.collection("users").document(userId).collection("bank_accounts")
                .document(accountId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        currentAccount = documentSnapshot.toObject(BankAccount.class);
                        if (currentAccount != null) {
                            // Ensure ID is set
                            currentAccount.setId(documentSnapshot.getId());
                            
                            // Populate fields with current data
                            populateFields();
                        } else {
                            Toast.makeText(this, "Error loading account data", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFields() {
        etAccountHolderName.setText(currentAccount.getAccountHolderName());
        etAccountNumber.setText(currentAccount.getAccountNumber());
        etBankName.setText(currentAccount.getBankName());
        etIfscCode.setText(currentAccount.getIfscCode());
        
        // Set account type spinner selection
        ArrayAdapter adapter = (ArrayAdapter) spinnerAccountType.getAdapter();
        int position = adapter.getPosition(currentAccount.getAccountType());
        spinnerAccountType.setSelection(position >= 0 ? position : 0);
        
        // Set UPI ID if available
        if (currentAccount.getUpiId() != null) {
            etUpiId.setText(currentAccount.getUpiId());
        }
    }

    private void validateAndUpdateAccount() {
        // Get input values
        String accountHolderName = etAccountHolderName.getText().toString().trim();
        String accountNumber = etAccountNumber.getText().toString().trim();
        String accountType = spinnerAccountType.getSelectedItem().toString();
        String bankName = etBankName.getText().toString().trim();
        String ifscCode = etIfscCode.getText().toString().trim();
        String upiId = etUpiId.getText().toString().trim();
        
        // Always primary in single account mode
        boolean isPrimary = true;

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
        
        // Confirm if account number has been changed
        if (!accountNumber.equals(currentAccount.getAccountNumber())) {
            // Show confirmation dialog for account number change
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Account Number Change")
                .setMessage("You are changing your bank account number. This is a critical field and incorrect information may result in payment failures. Are you sure you want to proceed?")
                .setPositiveButton("Yes, I'm Sure", (dialog, which) -> {
                    // Continue with validation and update
                    continueValidationAndUpdate(accountHolderName, accountNumber, accountType, bankName, ifscCode, upiId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Reset the account number field to original value
                    etAccountNumber.setText(currentAccount.getAccountNumber());
                })
                .setCancelable(false)
                .show();
        } else {
            // Account number hasn't changed, continue with validation
            continueValidationAndUpdate(accountHolderName, accountNumber, accountType, bankName, ifscCode, upiId);
        }
    }
    
    private void continueValidationAndUpdate(String accountHolderName, String accountNumber, 
                                           String accountType, String bankName, 
                                           String ifscCode, String upiId) {
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
            Toast.makeText(this, "You must be logged in to update an account", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        updateAccount(userId, accountHolderName, accountNumber, accountType, bankName, ifscCode, upiId);
    }

    private void updateAccount(String userId, String accountHolderName, String accountNumber, 
                             String accountType, String bankName, String ifscCode, String upiId) {
        // Get reference to the account document
        DocumentReference accountRef = db.collection("users").document(userId)
                .collection("bank_accounts").document(accountId);
        
        // Update the account fields
        accountRef.update(
                "accountHolderName", accountHolderName,
                "accountNumber", accountNumber,
                "accountType", accountType,
                "bankName", bankName,
                "ifscCode", ifscCode,
                "upiId", upiId,
                "isPrimary", true,  // Always primary in single account mode
                "lastUpdated", Timestamp.now()
        ).addOnSuccessListener(aVoid -> {
            showLoading(false);
            Toast.makeText(EditBankAccountActivity.this, "Bank account updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(EditBankAccountActivity.this, "Failed to update bank account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnUpdateAccount.setEnabled(!isLoading);
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