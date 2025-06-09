package com.example.carrentaluser;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.example.carrentaluser.adapters.TransactionAdapter;
import com.example.carrentaluser.models.Transaction;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

public class WalletActivity extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = "WalletActivity";
    private static final String RAZORPAY_API_KEY = "rzp_test_Jrr3S8Z52c8foR"; // Test key - replace with production key when going live
    private static final int REQUEST_SELECT_BANK_ACCOUNT = 1001;

    private TextView balanceTextView;
    private MaterialButton addMoneyButton, withdrawButton, refreshTransactionsButton;
    private RecyclerView transactionsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;

    private double currentBalance = 0.0;
    private double addAmount = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Razorpay
        Checkout.preload(getApplicationContext());

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up button listeners
        setupButtonListeners();

        // Load wallet data
        loadWalletData();
    }

    private void initViews() {
        balanceTextView = findViewById(R.id.balanceTextView);
        addMoneyButton = findViewById(R.id.addMoneyButton);
        withdrawButton = findViewById(R.id.withdrawButton);
        refreshTransactionsButton = findViewById(R.id.refreshTransactionsButton);
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        
        // Get the empty state add money button
        MaterialButton emptyStateAddMoneyButton = findViewById(R.id.emptyStateAddMoneyButton);
        if (emptyStateAddMoneyButton != null) {
            emptyStateAddMoneyButton.setOnClickListener(v -> showAddMoneyDialog());
        }
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactionList);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Add dividers between items
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                transactionsRecyclerView.getContext(), LinearLayoutManager.VERTICAL);
        transactionsRecyclerView.addItemDecoration(dividerItemDecoration);
        
        transactionsRecyclerView.setAdapter(adapter);
        
        // Add animation to the RecyclerView
        transactionsRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupButtonListeners() {
        addMoneyButton.setOnClickListener(v -> showAddMoneyDialog());
        withdrawButton.setOnClickListener(v -> showWithdrawDialog());
        
        // Set up refresh transactions button
        refreshTransactionsButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                Toast.makeText(this, "Refreshing transactions...", Toast.LENGTH_SHORT).show();
                fetchAllTransactionsForUser(userId);
            }
        });
        
        // Optional: Add long press listener for debugging
        transactionsRecyclerView.setOnLongClickListener(v -> {
            if (adapter != null) {
                adapter.toggleTransactionIds();
                return true;
            }
            return false;
        });
    }

    private void loadWalletData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to view your wallet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);

        // First check for any old transactions to migrate
        migrateOldTransactions(userId);

        // Get wallet balance
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get wallet_balance field, default to 0 if not present
                        Object balanceObj = documentSnapshot.get("wallet_balance");
                        if (balanceObj != null) {
                            if (balanceObj instanceof Long) {
                                currentBalance = ((Long) balanceObj).doubleValue();
                            } else if (balanceObj instanceof Double) {
                                currentBalance = (Double) balanceObj;
                            }
                        }
                        
                        updateBalanceDisplay();
                        
                        // Load transactions with direct fetch method
                        fetchAllTransactionsForUser(userId);
                    } else {
                        // User document doesn't exist, create with default balance
                        createUserWithDefaultBalance(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(WalletActivity.this, "Failed to load wallet data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createUserWithDefaultBalance(String userId) {
        Log.d(TAG, "Creating or updating user wallet for: " + userId);
        showLoading(true);
        
        // Create a complete user data map - include any fields that might be required
        Map<String, Object> userData = new HashMap<>();
        userData.put("wallet_balance", 0.0);
        userData.put("uid", userId); // Ensure user ID is stored in the document
        userData.put("wallet_created_at", new Date()); // Add timestamp for wallet creation
        
        // First try to get the document to see if it exists
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User document exists, updating wallet balance");
                        
                        // Document exists, just update the wallet balance
                        db.collection("users").document(userId)
                                .update("wallet_balance", 0.0)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Wallet balance updated successfully");
                                    currentBalance = 0.0;
                                    updateBalanceDisplay();
                                    fetchAllTransactionsForUser(userId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update wallet balance", e);
                                    showLoading(false);
                                    Toast.makeText(WalletActivity.this, "Failed to update wallet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.d(TAG, "User document doesn't exist, creating new document");
                        
                        // Document doesn't exist, create it with set()
                        db.collection("users").document(userId)
                                .set(userData, SetOptions.merge()) // Use merge to avoid overwriting other fields
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User document created successfully with wallet balance");
                                    currentBalance = 0.0;
                                    updateBalanceDisplay();
                                    fetchAllTransactionsForUser(userId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to create user document", e);
                                    showLoading(false);
                                    Toast.makeText(WalletActivity.this, "Failed to create wallet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    
                                    // Try alternative approach if set fails
                                    attemptAlternativeUserCreation(userId);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check if user document exists", e);
                    showLoading(false);
                    Toast.makeText(WalletActivity.this, "Failed to check user document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Try alternative approach
                    attemptAlternativeUserCreation(userId);
                });
    }
    
    // Fallback method if regular user creation fails
    private void attemptAlternativeUserCreation(String userId) {
        Log.d(TAG, "Attempting alternative user document creation");
        
        // Simplified data with only necessary fields
        Map<String, Object> minimalData = new HashMap<>();
        minimalData.put("wallet_balance", 0.0);
        
        // Try with a simple update operation which will create the field if missing
        db.collection("users").document(userId)
                .set(minimalData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                                                        Log.d(TAG, "Alternative user document creation successful");
                                    currentBalance = 0.0;
                                    updateBalanceDisplay();
                                    fetchAllTransactionsForUser(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Alternative user creation also failed", e);
                    showLoading(false);
                    Toast.makeText(WalletActivity.this, 
                            "Could not create wallet. Please restart the app and try again.", 
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadTransactions(String userId) {
        Log.d(TAG, "Loading transactions for user: " + userId);
        showLoading(true);
        
        // Use a path that stores transactions as a subcollection of users
        db.collection("users").document(userId).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No transactions found for user");
                        transactionList.clear();
                        updateTransactionList(transactionList);
                        return;
                    }
                    
                    List<Transaction> newTransactions = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Log.d(TAG, "Processing transaction: " + document.getId());
                            
                            // Create transaction object manually with safe defaults
                            String id = document.getId();
                            String type = document.getString("type") != null ? document.getString("type") : "unknown";
                            String description = document.getString("description") != null ? document.getString("description") : "";
                            String status = document.getString("status") != null ? document.getString("status") : "completed";
                            
                            // Handle timestamp which might be missing
                            Date timestamp;
                            try {
                                timestamp = document.getDate("timestamp");
                                if (timestamp == null) {
                                    timestamp = new Date();
                                }
                            } catch (Exception dateEx) {
                                timestamp = new Date();
                                Log.e(TAG, "Error parsing timestamp", dateEx);
                            }
                            
                            // Handle amount which could be in different formats
                            double amount = 0.0;
                            Object amountObj = document.get("amount");
                            if (amountObj != null) {
                                try {
                                    if (amountObj instanceof Long) {
                                        amount = ((Long) amountObj).doubleValue();
                                    } else if (amountObj instanceof Double) {
                                        amount = (Double) amountObj;
                                    } else if (amountObj instanceof String) {
                                        amount = Double.parseDouble((String) amountObj);
                                    } else if (amountObj instanceof Integer) {
                                        amount = ((Integer) amountObj).doubleValue();
                                    }
                                } catch (Exception amountEx) {
                                    Log.e(TAG, "Error parsing amount", amountEx);
                                }
                            }
                            
                            Transaction transaction = new Transaction(
                                    id,
                                    userId, // Since we're querying user's transactions, userId is known
                                    type,
                                    description,
                                    amount,
                                    timestamp,
                                    status
                            );
                            
                            newTransactions.add(transaction);
                            Log.d(TAG, "Added transaction: " + transaction.getId() + ", type: " + transaction.getType() + ", amount: " + transaction.getAmount());
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing transaction document: " + document.getId(), ex);
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + newTransactions.size() + " transactions");
                    
                    // Update the transaction list and UI
                    transactionList.clear();
                    transactionList.addAll(newTransactions);
                    updateTransactionList(transactionList);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading transactions", e);
                    Toast.makeText(WalletActivity.this, "Failed to load transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    transactionList.clear();
                    updateTransactionList(transactionList);
                });
    }

    private void updateBalanceDisplay() {
        balanceTextView.setText(String.format("₹ %.2f", currentBalance));
    }

    private void showAddMoneyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Money to Wallet");
        
        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);
        layout.addView(input);
        
        builder.setView(layout);
        
        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        Toast.makeText(WalletActivity.this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (amount < 1.0) {
                        Toast.makeText(WalletActivity.this, "Minimum amount is ₹1", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Store the amount for later use
                    addAmount = amount;
                    
                    // Start Razorpay payment
                    startRazorpayPayment(amount);
                    
                } catch (NumberFormatException e) {
                    Toast.makeText(WalletActivity.this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(WalletActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void startRazorpayPayment(double amount) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to add money", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Convert amount to paise (1 INR = 100 paise)
        int amountInPaise = (int) (amount * 100);
        
        // Initialize Razorpay checkout
        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_API_KEY);
        
        try {
            // Get user details
            String userId = mAuth.getCurrentUser().getUid();
            String userEmail = mAuth.getCurrentUser().getEmail();
            
            // Create options JSON object
            JSONObject options = new JSONObject();
            
            // Set amount in paise
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("name", "Car Rental");
            options.put("description", "Add money to wallet");
            options.put("image", "https://example.com/your_logo.png"); // Replace with your logo URL
            // For production, leave order_id empty as it will be generated by Razorpay
            // options.put("order_id", "");
            
            JSONObject prefill = new JSONObject();
            if (userEmail != null && !userEmail.isEmpty()) {
                prefill.put("email", userEmail);
            }
            
            // Fetch user's phone if available
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String phone = documentSnapshot.getString("mobile_number");
                            if (phone != null && !phone.isEmpty()) {
                                try {
                                    prefill.put("contact", phone);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error adding phone to prefill", e);
                                }
                            }
                            
                            try {
                                options.put("prefill", prefill);
                                
                                // Theme customization
                                JSONObject theme = new JSONObject();
                                theme.put("color", "#3F51B5"); // Primary color
                                options.put("theme", theme);
                                
                                // Start payment
                                checkout.open(WalletActivity.this, options);
                            } catch (Exception e) {
                                Log.e(TAG, "Error in Razorpay options", e);
                                Toast.makeText(WalletActivity.this, "Payment error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Continue without phone number
                        try {
                            options.put("prefill", prefill);
                            
                            // Theme customization
                            JSONObject theme = new JSONObject();
                            theme.put("color", "#3F51B5"); // Primary color
                            options.put("theme", theme);
                            
                            // Start payment
                            checkout.open(WalletActivity.this, options);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error in Razorpay options", ex);
                            Toast.makeText(WalletActivity.this, "Payment error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting Razorpay payment", e);
            Toast.makeText(this, "Error starting payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showWithdrawDialog() {
        // First check if user has any bank accounts
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to withdraw money", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        
        // Check if user has any bank accounts
        db.collection("users").document(userId).collection("bank_accounts")
                .whereEqualTo("isPrimary", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No primary bank account found, check if any accounts exist
                        db.collection("users").document(userId).collection("bank_accounts")
                                .limit(1)
                                .get()
                                .addOnSuccessListener(allAccountsSnapshot -> {
                                    if (allAccountsSnapshot.isEmpty()) {
                                        // No bank accounts at all
                                        showAddBankAccountDialog();
                                    } else {
                                        // Has accounts but none set as primary
                                        startBankAccountSelection();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(WalletActivity.this, "Failed to check bank accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        return;
                    }
                    
                    // Primary bank account exists, proceed with withdrawal
                    String bankAccountId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    showAmountInputDialog(bankAccountId);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to check bank accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showAddBankAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bank Account Required");
        builder.setMessage("You need to add a bank account before you can withdraw money. Would you like to add one now?");
        
        builder.setPositiveButton("Add Bank Account", (dialog, which) -> {
            Intent intent = new Intent(WalletActivity.this, AddBankAccountActivity.class);
            startActivity(intent);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        builder.show();
    }
    
    private void startBankAccountSelection() {
        Intent intent = new Intent(WalletActivity.this, BankAccountActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_BANK_ACCOUNT);
    }
    
    private void showAmountInputDialog(String bankAccountId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Withdraw from Wallet");
        
        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount (min ₹100)");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);
        layout.addView(input);
        
        // Add a note about minimum withdrawal
        TextView noteText = new TextView(this);
        noteText.setText("Note: Minimum withdrawal amount is ₹100");
        noteText.setTextSize(12);
        noteText.setPadding(0, 8, 0, 0);
        layout.addView(noteText);
        
        builder.setView(layout);
        
        // Set up the buttons
        builder.setPositiveButton("Withdraw", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    // Check minimum withdrawal amount
                    if (amount < 100) {
                        Toast.makeText(WalletActivity.this, "Minimum withdrawal amount is ₹100", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (amount <= 0) {
                        Toast.makeText(WalletActivity.this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (amount > currentBalance) {
                        Toast.makeText(WalletActivity.this, "Insufficient balance", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    withdrawFromWallet(amount, bankAccountId);
                } catch (NumberFormatException e) {
                    Toast.makeText(WalletActivity.this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(WalletActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }

    private void withdrawFromWallet(double amount, String bankAccountId) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to withdraw money", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (amount < 100) {
            Toast.makeText(this, "Minimum withdrawal amount is ₹100", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (amount > currentBalance) {
            Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        
        // Fetch bank account details
        db.collection("users").document(userId).collection("bank_accounts")
                .document(bankAccountId)
                .get()
                .addOnSuccessListener(bankAccountDoc -> {
                    if (!bankAccountDoc.exists()) {
                        showLoading(false);
                        Toast.makeText(WalletActivity.this, "Bank account not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Get bank account details
                    String bankName = bankAccountDoc.getString("bankName");
                    String accountNumber = bankAccountDoc.getString("accountNumber");
                    
                    // Mask account number for display (show only last 4 digits)
                    String maskedNumber = "XXXX XXXX " + accountNumber.substring(Math.max(0, accountNumber.length() - 4));
                    
                    double newBalance = currentBalance - amount;
                    
                    // Generate a unique transaction ID
                    String transactionId = UUID.randomUUID().toString();
                    Log.d(TAG, "Generated transaction ID for withdrawal: " + transactionId);
                    
                    // Create transaction data with bank details
                    Map<String, Object> transactionData = new HashMap<>();
                    transactionData.put("type", "debit");
                    transactionData.put("description", "Withdrawn to " + bankName + " (" + maskedNumber + ")");
                    transactionData.put("amount", amount);
                    transactionData.put("timestamp", new Date());
                    transactionData.put("status", "processing"); // Initially set as processing
                    transactionData.put("bankAccountId", bankAccountId);
                    
                    // Start a batch operation
                    db.runTransaction(transaction -> {
                        // Update wallet balance
                        transaction.update(db.collection("users").document(userId), "wallet_balance", newBalance);
                        
                        // Save transaction
                        transaction.set(db.collection("users").document(userId).collection("transactions").document(transactionId), transactionData);
                        
                        // Create withdrawal request in admin collection for processing
                        Map<String, Object> withdrawalRequest = new HashMap<>(transactionData);
                        withdrawalRequest.put("userId", userId);
                        withdrawalRequest.put("transactionId", transactionId);
                        withdrawalRequest.put("bankAccountId", bankAccountId);
                        withdrawalRequest.put("requestTimestamp", new Date());
                        
                        transaction.set(db.collection("withdrawal_requests").document(transactionId), withdrawalRequest);
                        
                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        // Update local balance
                        currentBalance = newBalance;
                        updateBalanceDisplay();
                        
                        // Show success toast
                        Toast.makeText(WalletActivity.this, 
                            "Withdrawal of ₹" + String.format(Locale.getDefault(), "%.2f", amount) + " initiated", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Show detailed information dialog
                        showWithdrawalRequestedDialog(amount, bankName, maskedNumber);
                        
                        // Refresh transaction list
                        fetchAllTransactionsForUser(userId);
                        
                        showLoading(false);
                    }).addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(WalletActivity.this, "Failed to process withdrawal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to process withdrawal", e);
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(WalletActivity.this, "Failed to get bank account details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to get bank account details", e);
                });
    }
    
    private void showWithdrawalRequestedDialog(double amount, String bankName, String maskedAccountNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Withdrawal Requested");
        
        String message = "Your withdrawal request for ₹" + String.format(Locale.getDefault(), "%.2f", amount) + 
                " to " + bankName + " (" + maskedAccountNumber + ") has been initiated.\n\n" +
                "The amount will be credited to your bank account within 1-2 business days.";
        
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_SELECT_BANK_ACCOUNT && resultCode == RESULT_OK) {
            String selectedAccountId = data.getStringExtra("selected_account_id");
            if (selectedAccountId != null) {
                showAmountInputDialog(selectedAccountId);
            }
        }
    }

    // Razorpay payment success callback
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        Log.d(TAG, "Payment successful: " + razorpayPaymentId);
        
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        
        // First check if this payment ID already exists to avoid duplicates
        db.collection("users").document(userId).collection("transactions")
                .document(razorpayPaymentId)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        // Transaction already recorded, just update UI
                        showLoading(false);
                        Log.d(TAG, "Transaction already exists for payment ID: " + razorpayPaymentId);
                        Toast.makeText(WalletActivity.this, "Payment processed successfully", Toast.LENGTH_SHORT).show();
                        loadWalletData(); // Refresh all data
                        return;
                    }
                    
                    // Check if user exists before updating balance
                    db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                // Calculate new balance
                                double newBalance = currentBalance + addAmount;
                                Log.d(TAG, "Current balance: " + currentBalance + ", Adding: " + addAmount + ", New balance: " + newBalance);
                                
                                // If user document doesn't exist, create it first
                                if (!userDoc.exists()) {
                                    Log.d(TAG, "User document doesn't exist, creating it first");
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("wallet_balance", newBalance);
                                    userData.put("uid", userId);
                                    
                                    db.collection("users").document(userId)
                                            .set(userData, SetOptions.merge())
                                            .addOnSuccessListener(aVoid -> {
                                                saveTransaction(userId, razorpayPaymentId, "credit", "Added to wallet via Razorpay", addAmount);
                                            })
                                            .addOnFailureListener(e -> {
                                                showLoading(false);
                                                Log.e(TAG, "Failed to create user document", e);
                                                Toast.makeText(WalletActivity.this, "Failed to create user wallet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                    return;
                                }
                                
                                // User exists, update the balance
                                db.collection("users").document(userId)
                                        .update("wallet_balance", newBalance)
                                        .addOnSuccessListener(aVoid -> {
                                            saveTransaction(userId, razorpayPaymentId, "credit", "Added to wallet via Razorpay", addAmount);
                                        })
                                        .addOnFailureListener(e -> {
                                            showLoading(false);
                                            Toast.makeText(WalletActivity.this, "Failed to update wallet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "Failed to update wallet balance", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(WalletActivity.this, "Failed to check user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Failed to check user document", e);
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(WalletActivity.this, "Failed to check existing transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to check existing transaction", e);
                });
    }
    
    // Method to save a transaction in the user's subcollection
    private void saveTransaction(String userId, String transactionId, String type, String description, double amount) {
        Log.d(TAG, "Saving " + type + " transaction for user: " + userId + ", amount: " + amount);
        
        try {
            // Create transaction data
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("type", type);
            transactionData.put("description", description);
            transactionData.put("amount", amount);
            transactionData.put("timestamp", new Date());
            transactionData.put("status", "completed");
            
            // Save transaction in user's subcollection
            db.collection("users").document(userId).collection("transactions")
                    .document(transactionId)
                    .set(transactionData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Transaction saved successfully: " + transactionId);
                        
                        // Update UI
                        showLoading(false);
                        updateBalanceDisplay();
                        
                        // Show different UI based on transaction type
                        if (type.equals("credit")) {
                            // Show success message with animation
                            new AlertDialog.Builder(WalletActivity.this)
                                    .setTitle("Payment Successful")
                                    .setMessage("Your wallet has been credited with ₹" + String.format(Locale.getDefault(), "%.2f", amount))
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else if (type.equals("debit")) {
                            // Show toast for debit transactions
                            Toast.makeText(WalletActivity.this, 
                                "₹" + String.format(Locale.getDefault(), "%.2f", amount) + " withdrawn successfully", 
                                Toast.LENGTH_SHORT).show();
                        }
                        
                        // Force refresh transaction list
                        fetchAllTransactionsForUser(userId);
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Error saving transaction: " + transactionId, e);
                        
                        // Show error dialog
                        new AlertDialog.Builder(WalletActivity.this)
                                .setTitle("Transaction Failed")
                                .setMessage("Could not save transaction: " + e.getMessage())
                                .setPositiveButton("OK", null)
                                .show();
                        
                        // Refresh transaction list anyway
                        fetchAllTransactionsForUser(userId);
                    });
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "Error in saveTransaction", e);
            
            // Show error dialog
            new AlertDialog.Builder(WalletActivity.this)
                    .setTitle("Transaction Error")
                    .setMessage("An unexpected error occurred: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
            
            // Refresh transaction list anyway
            fetchAllTransactionsForUser(userId);
        }
    }

    // Razorpay payment error callback
    @Override
    public void onPaymentError(int code, String description) {
        Log.e(TAG, "Payment failed with code: " + code + ", description: " + description);
        
        // More detailed error handling based on error code
        String errorMessage;
        switch (code) {
            case Checkout.NETWORK_ERROR:
                errorMessage = "Network error. Please check your internet connection.";
                break;
            case Checkout.INVALID_OPTIONS:
                errorMessage = "Invalid payment options provided.";
                break;
            case Checkout.PAYMENT_CANCELED:
                errorMessage = "Payment was canceled.";
                break;
            case Checkout.TLS_ERROR:
                errorMessage = "TLS connection error. Please update your device.";
                break;
            default:
                errorMessage = "Payment Failed: " + description;
                break;
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        
        // Show error dialog with details
        new AlertDialog.Builder(this)
                .setTitle("Payment Failed")
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
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
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            loadWalletData();
            
            // Check for any pending refunds that might not have been credited
            checkPendingRefunds(userId);
            
            // Specifically check for wallet payment refunds
            checkWalletPaymentRefunds(userId);
        }
    }

    /**
     * Check for any bookings with refund_processed=true but where the refund might not have been
     * properly credited to the wallet balance
     */
    private void checkPendingRefunds(String userId) {
        db.collection("bookings")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("refund_processed", true)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    return; // No refunds to process
                }
                
                // For each booking with a processed refund
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    // Check if the refund was marked as credited to wallet
                    Boolean creditedToWallet = doc.getBoolean("credited_to_wallet");
                    
                    // If not explicitly marked as credited, or if it's false
                    if (creditedToWallet == null || !creditedToWallet) {
                        // Get refund amount
                        Number refundAmount = doc.getLong("refund_amount");
                        String bookingId = doc.getId();
                        
                        if (refundAmount != null && refundAmount.intValue() > 0) {
                            // Create transaction and update wallet balance
                            processRefundToWallet(userId, bookingId, refundAmount.intValue(), doc.getString("car_name"));
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking pending refunds: " + e.getMessage(), e);
            });
    }
    
    /**
     * Process a refund to the user's wallet
     */
    private void processRefundToWallet(String userId, String bookingId, int amount, String carName) {
        // Get current wallet balance
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(userDoc -> {
                if (!userDoc.exists()) {
                    Log.e(TAG, "User document not found when processing refund");
                    return;
                }
                
                // Get current balance
                double currentBalance = 0;
                if (userDoc.contains("wallet_balance")) {
                    Object balanceObj = userDoc.get("wallet_balance");
                    if (balanceObj instanceof Long) {
                        currentBalance = ((Long) balanceObj).doubleValue();
                    } else if (balanceObj instanceof Double) {
                        currentBalance = (Double) balanceObj;
                    } else if (balanceObj instanceof Integer) {
                        currentBalance = ((Integer) balanceObj).doubleValue();
                    }
                }
                
                // Calculate new balance
                final double newBalance = currentBalance + amount;
                
                // Create a batch for atomic operations
                WriteBatch batch = db.batch();
                
                // 1. Update wallet balance
                batch.update(db.collection("users").document(userId), 
                    "wallet_balance", newBalance);
                
                // 2. Create wallet transaction
                String transactionId = "trans_refund_recovery_" + System.currentTimeMillis();
                Map<String, Object> transactionData = new HashMap<>();
                transactionData.put("userId", userId);
                transactionData.put("type", "credit");
                transactionData.put("description", "Refund recovery for booking: " + carName);
                transactionData.put("amount", amount);
                transactionData.put("timestamp", new Date());
                transactionData.put("status", "completed");
                transactionData.put("related_booking_id", bookingId);
                
                // Add transaction to both collections
                batch.set(
                    db.collection("users").document(userId)
                      .collection("transactions").document(transactionId), 
                    transactionData
                );
                
                batch.set(
                    db.collection("transactions").document(transactionId),
                    transactionData
                );
                
                // 3. Update booking to mark refund as credited to wallet
                batch.update(db.collection("bookings").document(bookingId),
                    "credited_to_wallet", true
                );
                
                // Commit all operations as a batch
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Recovered refund of " + amount + " successfully credited to wallet");
                        // Reload wallet data to show updated balance
                        loadWalletData();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to process refund recovery: " + e.getMessage(), e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user data for refund recovery: " + e.getMessage(), e);
            });
    }

    // Helper method to update the transaction list
    private void updateTransactionList(List<Transaction> transactions) {
        // Update the adapter with the new transactions
        if (adapter != null) {
            adapter.updateTransactions(transactions);
            
            // Show empty state or transactions based on list size
            if (transactions.isEmpty()) {
                // Use animation to show empty state
                transactionsRecyclerView.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            transactionsRecyclerView.setVisibility(View.GONE);
                            
                            // Show empty state with animation
                            emptyStateLayout.setAlpha(0f);
                            emptyStateLayout.setVisibility(View.VISIBLE);
                            emptyStateLayout.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                        })
                        .start();
            } else {
                // Use animation to show transactions
                emptyStateLayout.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            emptyStateLayout.setVisibility(View.GONE);
                            
                            // Show transactions with animation
                            transactionsRecyclerView.setAlpha(0f);
                            transactionsRecyclerView.setVisibility(View.VISIBLE);
                            transactionsRecyclerView.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                        })
                        .start();
            }
        }
    }

    // Method to check for and migrate old transactions to new structure
    private void migrateOldTransactions(String userId) {
        Log.d(TAG, "Checking for old transactions to migrate for user: " + userId);
        
        // Check the old transactions collection
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No old transactions found to migrate");
                        return;
                    }
                    
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " old transactions to migrate");
                    
                    // Migrate each transaction to the new structure
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String id = document.getId();
                            String type = document.getString("type");
                            String description = document.getString("description");
                            String status = document.getString("status");
                            Date timestamp = document.getDate("timestamp");
                            
                            if (timestamp == null) {
                                timestamp = new Date();
                            }
                            
                            double amount = 0.0;
                            Object amountObj = document.get("amount");
                            if (amountObj != null) {
                                if (amountObj instanceof Long) {
                                    amount = ((Long) amountObj).doubleValue();
                                } else if (amountObj instanceof Double) {
                                    amount = (Double) amountObj;
                                } else if (amountObj instanceof String) {
                                    amount = Double.parseDouble((String) amountObj);
                                } else if (amountObj instanceof Integer) {
                                    amount = ((Integer) amountObj).doubleValue();
                                }
                            }
                            
                            // Skip invalid transactions
                            if (type == null) {
                                Log.w(TAG, "Skipping migration of invalid transaction: " + id);
                                continue;
                            }
                            
                            // Create transaction data for the new location
                            Map<String, Object> transactionData = new HashMap<>();
                            transactionData.put("type", type);
                            transactionData.put("description", description != null ? description : "");
                            transactionData.put("amount", amount);
                            transactionData.put("timestamp", timestamp);
                            transactionData.put("status", status != null ? status : "completed");
                            
                            // Save to the new location
                            db.collection("users").document(userId).collection("transactions")
                                    .document(id)
                                    .set(transactionData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Successfully migrated transaction: " + id);
                                        
                                        // Delete from old location
                                        db.collection("transactions").document(id).delete()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Log.d(TAG, "Deleted old transaction: " + id);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to delete old transaction: " + id, e);
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to migrate transaction: " + id, e);
                                    });
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error migrating transaction: " + document.getId(), e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for old transactions", e);
                });
    }

    // Method to fetch all transactions for a specific user
    private void fetchAllTransactionsForUser(String userId) {
        Log.d(TAG, "Directly fetching all transactions for user: " + userId);
        showLoading(true);
        
        // Clear existing transactions
        transactionList.clear();
        
        // Get all transactions for this user
        db.collection("users").document(userId).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    
                    int count = queryDocumentSnapshots.size();
                    Log.d(TAG, "Found " + count + " transactions for user: " + userId);
                    
                    if (count == 0) {
                        Log.d(TAG, "No transactions found");
                        updateTransactionList(new ArrayList<>()); // Empty list
                        return;
                    }
                    
                    List<Transaction> transactions = new ArrayList<>();
                    
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Log.d(TAG, "Processing transaction document: " + document.getId());
                            
                            // Map Firestore document to Transaction object
                            String id = document.getId();
                            String type = document.getString("type");
                            String description = document.getString("description");
                            Date timestamp = document.getDate("timestamp");
                            String status = document.getString("status");
                            
                            // Handle amount - might be stored in different formats
                            double amount = 0;
                            Object amountObj = document.get("amount");
                            if (amountObj instanceof Long) {
                                amount = ((Long) amountObj).doubleValue();
                            } else if (amountObj instanceof Double) {
                                amount = (Double) amountObj;
                            } else if (amountObj instanceof String) {
                                try {
                                    amount = Double.parseDouble((String) amountObj);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error parsing amount string: " + amountObj);
                                }
                            }
                            
                            // Set default values for null fields
                            if (type == null) type = "unknown";
                            if (description == null) description = "Transaction";
                            if (timestamp == null) timestamp = new Date();
                            if (status == null) status = "completed";
                            
                            // Create transaction object
                            Transaction transaction = new Transaction(
                                    id,
                                    userId,
                                    type,
                                    description,
                                    amount,
                                    timestamp,
                                    status
                            );
                            
                            transactions.add(transaction);
                            Log.d(TAG, "Added transaction: id=" + id + ", type=" + type + ", amount=" + amount);
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing transaction document: " + document.getId(), e);
                        }
                    }
                    
                    // Update UI with transactions
                    Log.d(TAG, "Successfully loaded " + transactions.size() + " transactions");
                    updateTransactionList(transactions);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error fetching transactions", e);
                    Toast.makeText(WalletActivity.this, "Failed to fetch transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateTransactionList(new ArrayList<>()); // Empty list
                });
    }

    /**
     * Check specifically for bookings that were paid with wallet but might not have been refunded properly
     */
    private void checkWalletPaymentRefunds(String userId) {
        db.collection("bookings")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("payment_method", "wallet")
            .whereEqualTo("status", "Cancelled")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    return; // No wallet payment bookings to process
                }
                
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " cancelled wallet payment bookings to check");
                
                // For each cancelled booking paid with wallet
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    // Check if the refund was marked as credited to wallet
                    Boolean creditedToWallet = doc.getBoolean("credited_to_wallet");
                    Boolean refundProcessed = doc.getBoolean("refund_processed");
                    
                    // If not explicitly marked as credited and refund not processed
                    if ((creditedToWallet == null || !creditedToWallet) && 
                        (refundProcessed == null || !refundProcessed)) {
                        
                        // Get refund amount (should be the advance payment amount)
                        Number refundAmount = doc.getLong("advance_payment_amount");
                        String bookingId = doc.getId();
                        String carName = doc.getString("car_name");
                        
                        if (refundAmount != null && refundAmount.intValue() > 0) {
                            Log.d(TAG, "Processing missed wallet refund for booking " + bookingId + ", amount: " + refundAmount);
                            // Create transaction and update wallet balance
                            processWalletPaymentRefund(userId, bookingId, refundAmount.intValue(), carName != null ? carName : "Unknown car");
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking wallet payment refunds: " + e.getMessage(), e);
            });
    }
    
    /**
     * Process a refund specifically for wallet payments
     */
    private void processWalletPaymentRefund(String userId, String bookingId, int amount, String carName) {
        // Get current wallet balance
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(userDoc -> {
                if (!userDoc.exists()) {
                    Log.e(TAG, "User document not found when processing wallet payment refund");
                    return;
                }
                
                // Get current balance
                double currentBalance = 0;
                if (userDoc.contains("wallet_balance")) {
                    Object balanceObj = userDoc.get("wallet_balance");
                    if (balanceObj instanceof Long) {
                        currentBalance = ((Long) balanceObj).doubleValue();
                    } else if (balanceObj instanceof Double) {
                        currentBalance = (Double) balanceObj;
                    } else if (balanceObj instanceof Integer) {
                        currentBalance = ((Integer) balanceObj).doubleValue();
                    }
                }
                
                // Calculate new balance
                final double newBalance = currentBalance + amount;
                
                // Create a batch for atomic operations
                WriteBatch batch = db.batch();
                
                // 1. Update wallet balance
                batch.update(db.collection("users").document(userId), 
                    "wallet_balance", newBalance);
                
                // 2. Create wallet transaction
                String transactionId = "trans_wallet_refund_" + System.currentTimeMillis();
                Map<String, Object> transactionData = new HashMap<>();
                transactionData.put("userId", userId);
                transactionData.put("type", "credit");
                transactionData.put("description", "Refund for wallet payment: " + carName);
                transactionData.put("amount", amount);
                transactionData.put("timestamp", new Date());
                transactionData.put("status", "completed");
                transactionData.put("related_booking_id", bookingId);
                transactionData.put("payment_method", "wallet");
                
                // Add transaction to both collections
                batch.set(
                    db.collection("users").document(userId)
                      .collection("transactions").document(transactionId), 
                    transactionData
                );
                
                batch.set(
                    db.collection("transactions").document(transactionId),
                    transactionData
                );
                
                // 3. Update booking to mark refund as processed and credited to wallet
                batch.update(db.collection("bookings").document(bookingId),
                    "refund_processed", true,
                    "credited_to_wallet", true,
                    "refund_amount", amount,
                    "refund_date", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", 
                        java.util.Locale.getDefault()).format(new java.util.Date())
                );
                
                // Commit all operations as a batch
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Wallet payment refund of " + amount + " successfully credited to wallet");
                        // Show toast notification
                        Toast.makeText(WalletActivity.this, 
                            "₹" + amount + " credited to wallet for cancellation of " + carName, 
                            Toast.LENGTH_SHORT).show();
                        // Reload wallet data to show updated balance
                        loadWalletData();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to process wallet payment refund: " + e.getMessage(), e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user data for wallet payment refund: " + e.getMessage(), e);
            });
    }
} 