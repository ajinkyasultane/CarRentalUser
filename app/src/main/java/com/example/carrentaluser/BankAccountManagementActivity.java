package com.example.carrentaluser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.adapters.BankAccountManagementAdapter;
import com.example.carrentaluser.models.BankAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class BankAccountManagementActivity extends AppCompatActivity {

    private static final String TAG = "BankAccountManagement";
    
    private RecyclerView accountsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private FloatingActionButton addAccountFab;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BankAccountManagementAdapter adapter;
    private List<BankAccount> bankAccounts;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_account_management);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Bank Accounts");
        }
        
        // Initialize views
        accountsRecyclerView = findViewById(R.id.accountsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        addAccountFab = findViewById(R.id.addAccountFab);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup click listeners
        addAccountFab.setOnClickListener(v -> {
            Intent intent = new Intent(BankAccountManagementActivity.this, AddBankAccountActivity.class);
            startActivity(intent);
        });
        
        // Load bank accounts
        loadBankAccounts();
    }
    
    private void setupRecyclerView() {
        bankAccounts = new ArrayList<>();
        adapter = new BankAccountManagementAdapter(this, bankAccounts, 
            // Edit account click listener
            accountId -> {
                Intent intent = new Intent(BankAccountManagementActivity.this, EditBankAccountActivity.class);
                intent.putExtra("account_id", accountId);
                startActivity(intent);
            },
            // Delete account click listener
            this::deleteAccount,
            // Set as primary click listener
            this::setPrimaryAccount
        );
        
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        accountsRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        accountsRecyclerView.setAdapter(adapter);
    }
    
    private void loadBankAccounts() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to view your bank accounts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        
        // Use a simpler query that doesn't require a composite index
        db.collection("users").document(userId).collection("bank_accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }
                    
                    bankAccounts.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        BankAccount account = document.toObject(BankAccount.class);
                        if (account != null) {
                            // Ensure ID is set
                            account.setId(document.getId());
                            bankAccounts.add(account);
                        }
                    }
                    
                    // Sort the accounts locally instead of in the query
                    // Primary accounts first, then by last updated date
                    bankAccounts.sort((a1, a2) -> {
                        // First compare by isPrimary (true comes before false)
                        if (a1.isPrimary() && !a2.isPrimary()) {
                            return -1;
                        } else if (!a1.isPrimary() && a2.isPrimary()) {
                            return 1;
                        }
                        
                        // If both have same primary status, compare by lastUpdated
                        if (a1.getLastUpdated() != null && a2.getLastUpdated() != null) {
                            return a2.getLastUpdated().compareTo(a1.getLastUpdated()); // Descending order
                        }
                        
                        return 0;
                    });
                    
                    adapter.notifyDataSetChanged();
                    showEmptyState(bankAccounts.isEmpty());
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyState(true);
                    Log.e(TAG, "Error loading bank accounts", e);
                    Toast.makeText(this, "Failed to load bank accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void deleteAccount(BankAccount account) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to delete an account", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // Prevent deletion of primary account
        if (account.isPrimary()) {
            Toast.makeText(this, "Cannot delete primary account. Set another account as primary first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Bank Account")
            .setMessage("Are you sure you want to delete this bank account?\n\n" +
                    "Bank: " + account.getBankName() + "\n" +
                    "Account Number: XXXX XXXX " + account.getAccountNumber().substring(Math.max(0, account.getAccountNumber().length() - 4)))
            .setPositiveButton("Delete", (dialog, which) -> {
                // Delete the account from Firestore
                db.collection("users").document(userId).collection("bank_accounts")
                        .document(account.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            
                            // Remove from local list and update UI
                            int position = bankAccounts.indexOf(account);
                            if (position != -1) {
                                bankAccounts.remove(position);
                                adapter.notifyItemRemoved(position);
                                showEmptyState(bankAccounts.isEmpty());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void setPrimaryAccount(BankAccount account) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to set a primary account", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // If already primary, do nothing
        if (account.isPrimary()) {
            Toast.makeText(this, "This is already your primary account", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Set as Primary Account")
            .setMessage("Are you sure you want to set this as your primary bank account?\n\n" +
                    "Bank: " + account.getBankName() + "\n" +
                    "Account Number: XXXX XXXX " + account.getAccountNumber().substring(Math.max(0, account.getAccountNumber().length() - 4)))
            .setPositiveButton("Set as Primary", (dialog, which) -> {
                // First, find the current primary account and update it
                for (BankAccount existingAccount : bankAccounts) {
                    if (existingAccount.isPrimary()) {
                        // Update the current primary account to not be primary
                        db.collection("users").document(userId).collection("bank_accounts")
                                .document(existingAccount.getId())
                                .update("isPrimary", false);
                        
                        // Update the local object
                        existingAccount.setPrimary(false);
                    }
                }
                
                // Set the new account as primary
                db.collection("users").document(userId).collection("bank_accounts")
                        .document(account.getId())
                        .update("isPrimary", true)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Primary account updated", Toast.LENGTH_SHORT).show();
                            
                            // Update the local object
                            account.setPrimary(true);
                            
                            // Refresh the entire list to ensure correct order
                            loadBankAccounts();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update primary account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        accountsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
    
    private void showEmptyState(boolean isEmpty) {
        emptyStateTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        accountsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload accounts when returning to the activity
        loadBankAccounts();
    }
}
