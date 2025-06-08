package com.example.carrentaluser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.adapters.BankAccountAdapter;
import com.example.carrentaluser.models.BankAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class BankAccountActivity extends AppCompatActivity {

    private static final String TAG = "BankAccountActivity";
    
    private RecyclerView accountsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private FloatingActionButton addAccountFab;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BankAccountAdapter adapter;
    private List<BankAccount> bankAccounts;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_account);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Bank Accounts");
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
            Intent intent = new Intent(BankAccountActivity.this, AddBankAccountActivity.class);
            startActivity(intent);
        });
        
        // Load bank accounts
        loadBankAccounts();
    }
    
    private void setupRecyclerView() {
        bankAccounts = new ArrayList<>();
        adapter = new BankAccountAdapter(this, bankAccounts, accountId -> {
            // Handle account selection for withdrawal
            Intent intent = new Intent();
            intent.putExtra("selected_account_id", accountId);
            setResult(RESULT_OK, intent);
            finish();
        });
        
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
        
        db.collection("users").document(userId).collection("bank_accounts")
                .orderBy("isPrimary", Query.Direction.DESCENDING)  // Primary accounts first
                .orderBy("lastUpdated", Query.Direction.DESCENDING) // Then most recently updated
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