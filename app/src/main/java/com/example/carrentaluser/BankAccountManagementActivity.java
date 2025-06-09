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
import com.google.firebase.Timestamp;

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
            getSupportActionBar().setTitle("Bank Account");
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
            // Delete account click listener - not used in single account mode
            account -> {},
            // Set as primary click listener - not used in single account mode
            account -> {}
        );
        
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        accountsRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        accountsRecyclerView.setAdapter(adapter);
    }
    
    private void loadBankAccounts() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to view your bank account", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        
        db.collection("users").document(userId).collection("bank_accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState(true);
                        addAccountFab.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    bankAccounts.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        BankAccount account = document.toObject(BankAccount.class);
                        if (account != null) {
                            // Ensure ID is set
                            account.setId(document.getId());
                            // Ensure account is marked as primary
                            account.setPrimary(true);
                            bankAccounts.add(account);
                            
                            // If account is not already marked as primary in the database, update it
                            if (!account.isPrimary()) {
                                db.collection("users").document(userId)
                                  .collection("bank_accounts").document(account.getId())
                                  .update("isPrimary", true);
                            }
                            
                            break; // Only get the first account
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    showEmptyState(bankAccounts.isEmpty());
                    
                    // Hide the FAB if we already have an account
                    if (!bankAccounts.isEmpty()) {
                        addAccountFab.setVisibility(View.GONE);
                    } else {
                        addAccountFab.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyState(true);
                    Log.e(TAG, "Error loading bank accounts", e);
                    Toast.makeText(this, "Failed to load bank account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
