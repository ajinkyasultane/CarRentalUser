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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.UUID;

public class AddBankAccountActivity extends AppCompatActivity {

    private static final String TAG = "AddBankAccountActivity";
    
    private EditText accountHolderNameEditText;
    private EditText accountNumberEditText;
    private EditText confirmAccountNumberEditText;
    private EditText ifscCodeEditText;
    private EditText bankNameEditText;
    private Spinner accountTypeSpinner;
    private EditText upiIdEditText;
    private CheckBox setPrimaryCheckBox;
    private Button saveButton;
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
        initViews();
        
        // Setup spinner for account types
        setupAccountTypeSpinner();
        
        // Setup save button
        saveButton.setOnClickListener(v -> validateAndSaveAccount());
    }
    
    private void initViews() {
        accountHolderNameEditText = findViewById(R.id.accountHolderNameEditText);
        accountNumberEditText = findViewById(R.id.accountNumberEditText);
        confirmAccountNumberEditText = findViewById(R.id.confirmAccountNumberEditText);
        ifscCodeEditText = findViewById(R.id.ifscCodeEditText);
        bankNameEditText = findViewById(R.id.bankNameEditText);
        accountTypeSpinner = findViewById(R.id.accountTypeSpinner);
        upiIdEditText = findViewById(R.id.upiIdEditText);
        setPrimaryCheckBox = findViewById(R.id.setPrimaryCheckBox);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupAccountTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.account_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountTypeSpinner.setAdapter(adapter);
    }
    
    private void validateAndSaveAccount() {
        // Hide keyboard
        // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        // imm.hideSoftInputFromWindow(saveButton.getWindowToken(), 0);
        
        // Get input values
        String accountHolderName = accountHolderNameEditText.getText().toString().trim();
        String accountNumber = accountNumberEditText.getText().toString().trim();
        String confirmAccountNumber = confirmAccountNumberEditText.getText().toString().trim();
        String ifscCode = ifscCodeEditText.getText().toString().trim().toUpperCase();
        String bankName = bankNameEditText.getText().toString().trim();
        String accountType = accountTypeSpinner.getSelectedItem().toString();
        String upiId = upiIdEditText.getText().toString().trim();
        boolean isPrimary = setPrimaryCheckBox.isChecked();
        
        // Validate inputs
        if (TextUtils.isEmpty(accountHolderName)) {
            accountHolderNameEditText.setError("Account holder name is required");
            return;
        }
        
        if (TextUtils.isEmpty(accountNumber)) {
            accountNumberEditText.setError("Account number is required");
            return;
        }
        
        if (!accountNumber.equals(confirmAccountNumber)) {
            confirmAccountNumberEditText.setError("Account numbers do not match");
            return;
        }
        
        if (TextUtils.isEmpty(ifscCode)) {
            ifscCodeEditText.setError("IFSC code is required");
            return;
        }
        
        if (TextUtils.isEmpty(bankName)) {
            bankNameEditText.setError("Bank name is required");
            return;
        }
        
        // Basic validation for account number (should be at least 8 digits)
        if (accountNumber.length() < 8) {
            accountNumberEditText.setError("Account number should be at least 8 digits");
            return;
        }
        
        // Basic validation for IFSC code (should be 11 characters)
        if (ifscCode.length() != 11) {
            ifscCodeEditText.setError("IFSC code should be 11 characters");
            return;
        }
        
        // All validations passed, save the account
        saveAccount(accountHolderName, accountNumber, ifscCode, bankName, accountType, upiId, isPrimary);
    }
    
    private void saveAccount(String accountHolderName, String accountNumber, String ifscCode, 
                              String bankName, String accountType, String upiId, boolean isPrimary) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to add a bank account", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        
        // Check if this is the first account, if so, make it primary
        db.collection("users").document(userId).collection("bank_accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isFirstAccount = queryDocumentSnapshots.isEmpty();
                    
                    // If this is the first account or user wants to set it as primary
                    boolean shouldBePrimary = isFirstAccount || isPrimary;
                    
                    // If this should be the primary account, update any existing primary account
                    if (shouldBePrimary && !isFirstAccount) {
                        // Find the current primary account and update it
                        queryDocumentSnapshots.getDocuments().forEach(document -> {
                            if (document.getBoolean("isPrimary") != null && document.getBoolean("isPrimary")) {
                                db.collection("users").document(userId).collection("bank_accounts")
                                        .document(document.getId())
                                        .update("isPrimary", false);
                            }
                        });
                    }
                    
                    // Create new bank account
                    String accountId = UUID.randomUUID().toString();
                    Date now = new Date();
                    
                    BankAccount bankAccount = new BankAccount(
                            accountId,
                            userId,
                            accountHolderName,
                            accountNumber,
                            ifscCode,
                            bankName,
                            accountType,
                            upiId,
                            now,
                            now,
                            false, // Not verified initially
                            shouldBePrimary
                    );
                    
                    // Save to Firestore
                    db.collection("users").document(userId).collection("bank_accounts")
                            .document(accountId)
                            .set(bankAccount)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(AddBankAccountActivity.this, "Bank account added successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Log.e(TAG, "Error adding bank account", e);
                                Toast.makeText(AddBankAccountActivity.this, "Failed to add bank account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error checking existing accounts", e);
                    Toast.makeText(AddBankAccountActivity.this, "Failed to check existing accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!isLoading);
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