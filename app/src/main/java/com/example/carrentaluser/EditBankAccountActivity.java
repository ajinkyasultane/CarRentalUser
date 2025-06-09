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
        
        // Set primary checkbox
        cbSetAsPrimary.setChecked(currentAccount.isPrimary());
        
        // Disable primary checkbox if already primary
        if (currentAccount.isPrimary()) {
            cbSetAsPrimary.setEnabled(false);
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
        boolean isPrimary = cbSetAsPrimary.isChecked();

        // Validate inputs
        if (TextUtils.isEmpty(accountHolderName)) {
            etAccountHolderName.setError("Account holder name is required");
            etAccountHolderName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(accountNumber)) {
            etAccountNumber.setError("Account number is required");
            etAccountNumber.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(bankName)) {
            etBankName.setError("Bank name is required");
            etBankName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(ifscCode)) {
            etIfscCode.setError("IFSC code is required");
            etIfscCode.requestFocus();
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

        // If setting as primary and wasn't primary before, update any existing primary account first
        if (isPrimary && !currentAccount.isPrimary()) {
            db.collection("users").document(userId).collection("bank_accounts")
                    .whereEqualTo("isPrimary", true)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // Update any existing primary accounts
                        for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                            db.collection("users").document(userId).collection("bank_accounts")
                                    .document(queryDocumentSnapshots.getDocuments().get(i).getId())
                                    .update("isPrimary", false);
                        }
                        
                        // Now update the current account
                        updateAccount(userId, accountHolderName, accountNumber, accountType, bankName, ifscCode, upiId, isPrimary);
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(EditBankAccountActivity.this, "Failed to check existing accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If not changing primary status, just update the account
            updateAccount(userId, accountHolderName, accountNumber, accountType, bankName, ifscCode, upiId, isPrimary);
        }
    }

    private void updateAccount(String userId, String accountHolderName, String accountNumber, 
                             String accountType, String bankName, String ifscCode, 
                             String upiId, boolean isPrimary) {
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
                "isPrimary", isPrimary,
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