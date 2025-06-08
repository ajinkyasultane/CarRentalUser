package com.example.carrentaluser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import org.json.JSONObject;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentMethodActivity extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = "PaymentMethodActivity";
    
    // UI components
    private TextView tvCarName, tvPaymentAmount, tvPaymentDescription, tvWalletBalance;
    private ImageView ivCarImage;
    private Button btnProceedPayment;
    private RadioGroup paymentMethodGroup;
    private RadioButton radioRazorpay, radioWallet;
    private CardView walletCardView;
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Payment details
    private boolean isAdvancePayment = false;
    private int totalPrice = 0;
    private String carName;
    private String carImage;
    private int amount;
    private String bookingId;
    private double walletBalance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_method);
        
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
            getSupportActionBar().setTitle("Choose Payment Method");
        }

        // Initialize views
        initViews();
        
        // Get intent data
        getIntentData();
        
        // Load user wallet balance
        loadWalletBalance();
        
        // Set up listeners
        setupListeners();
    }
    
    private void initViews() {
        tvCarName = findViewById(R.id.tv_car_name);
        tvPaymentAmount = findViewById(R.id.tv_payment_amount);
        tvPaymentDescription = findViewById(R.id.tv_payment_description);
        tvWalletBalance = findViewById(R.id.tv_wallet_balance);
        ivCarImage = findViewById(R.id.iv_car_image);
        btnProceedPayment = findViewById(R.id.btn_proceed_payment);
        paymentMethodGroup = findViewById(R.id.payment_method_group);
        radioRazorpay = findViewById(R.id.radio_razorpay);
        radioWallet = findViewById(R.id.radio_wallet);
        walletCardView = findViewById(R.id.wallet_balance_card);
    }
    
    private void getIntentData() {
        carName = getIntent().getStringExtra("car_name");
        carImage = getIntent().getStringExtra("car_image");
        amount = getIntent().getIntExtra("amount", 0);
        isAdvancePayment = getIntent().getBooleanExtra("is_advance_payment", false);
        bookingId = getIntent().getStringExtra("booking_id");
        
        if (isAdvancePayment) {
            totalPrice = getIntent().getIntExtra("total_price", 0);
        }
        
        // Set data to views
        tvCarName.setText(carName);
        tvPaymentAmount.setText("₹" + amount);
        
        String description = isAdvancePayment ? 
                "50% Advance Payment for " + carName : 
                "Payment for " + carName;
        tvPaymentDescription.setText(description);
        
        // Load car image
        if (carImage != null && !carImage.isEmpty()) {
            Glide.with(this)
                    .load(carImage)
                    .placeholder(R.drawable.car_placeholder)
                    .error(R.drawable.car_placeholder)
                    .into(ivCarImage);
        }
    }
    
    private void loadWalletBalance() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get wallet_balance field
                        Object balanceObj = documentSnapshot.get("wallet_balance");
                        if (balanceObj != null) {
                            if (balanceObj instanceof Long) {
                                walletBalance = ((Long) balanceObj).doubleValue();
                            } else if (balanceObj instanceof Double) {
                                walletBalance = (Double) balanceObj;
                            } else if (balanceObj instanceof Integer) {
                                walletBalance = ((Integer) balanceObj).doubleValue();
                            }
                        }
                        
                        // Update UI
                        updateWalletBalanceUI();
                    } else {
                        // Create user with 0 balance
                        createUserWithDefaultBalance(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load wallet balance: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading wallet balance", e);
                });
    }
    
    private void createUserWithDefaultBalance(String userId) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("wallet_balance", 0.0);
        
        db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    walletBalance = 0.0;
                    updateWalletBalanceUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create wallet: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating wallet", e);
                });
    }
    
    private void updateWalletBalanceUI() {
        tvWalletBalance.setText(String.format(Locale.getDefault(), "₹%.2f", walletBalance));
        
        // Check if wallet has enough balance
        if (walletBalance < amount) {
            radioWallet.setEnabled(false);
            radioWallet.setText("Wallet (Insufficient Balance)");
            radioRazorpay.setChecked(true);
        } else {
            radioWallet.setEnabled(true);
            radioWallet.setText("Wallet");
        }
    }
    
    private void setupListeners() {
        btnProceedPayment.setOnClickListener(v -> {
            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
            
            if (selectedId == R.id.radio_razorpay) {
                // Proceed with Razorpay payment
                proceedToRazorpayPayment();
            } else if (selectedId == R.id.radio_wallet) {
                // Proceed with Wallet payment
                proceedToWalletPayment();
            }
        });
    }
    
    private void proceedToRazorpayPayment() {
        // Initialize Razorpay checkout directly
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_Jrr3S8Z52c8foR");
        
        try {
            // Create options JSON object
            JSONObject options = new JSONObject();
            
            // Set amount in paise (100 paise = 1 INR)
            options.put("amount", amount * 100);
            options.put("currency", "INR");
            
            // Add order details
            options.put("name", "Car Rental");
            
            String description = isAdvancePayment ? 
                    "50% Advance Payment for " + carName : 
                    "Payment for " + carName;
            options.put("description", description);
            
            // Prefill customer information if available
            JSONObject prefill = new JSONObject();
            
            // Get user email from Firebase Auth
            if (mAuth.getCurrentUser() != null) {
                String email = mAuth.getCurrentUser().getEmail();
                if (email != null && !email.isEmpty()) {
                    prefill.put("email", email);
                }
            }
            
            options.put("prefill", prefill);
            
            // Theme customization
            JSONObject theme = new JSONObject();
            theme.put("color", "#4CAF50");
            options.put("theme", theme);
            
            // Show dialog with test card info before proceeding
            new AlertDialog.Builder(this)
                    .setTitle("Test Payment Information")
                    .setMessage("You are about to make a test payment with Razorpay TEST MODE.\n\n" +
                            "Use these test card details:\n" +
                            "Card Number: 4111 1111 1111 1111\n" +
                            "Expiry: Any future date (e.g. 12/25)\n" +
                            "CVV: Any 3 digits (e.g. 123)\n" +
                            "Name: Any name\n\n" +
                            "For OTP: Use 1234 (or any number)")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        // Start Razorpay checkout
                        checkout.open(this, options);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting Razorpay payment", e);
            Toast.makeText(this, "Error starting payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void proceedToWalletPayment() {
        // Double check if wallet has enough balance
        if (walletBalance < amount) {
            Toast.makeText(this, "Insufficient wallet balance", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setMessage("Are you sure you want to pay ₹" + amount + " from your wallet?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Process wallet payment
                    processWalletPayment();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void processWalletPayment() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // Generate a unique transaction ID
        String transactionId = "wallet_" + System.currentTimeMillis();
        
        // Update wallet balance
        double newBalance = walletBalance - amount;
        
        // Start a transaction
        db.runTransaction(transaction -> {
            // Get current wallet balance
            DocumentSnapshot userDoc = transaction.get(db.collection("users").document(userId));
            
            if (!userDoc.exists()) {
                try {
                    throw new Exception("User document not found");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            
            // Double check if wallet has enough balance
            Object balanceObj = userDoc.get("wallet_balance");
            double currentBalance = 0.0;
            if (balanceObj instanceof Long) {
                currentBalance = ((Long) balanceObj).doubleValue();
            } else if (balanceObj instanceof Double) {
                currentBalance = (Double) balanceObj;
            } else if (balanceObj instanceof Integer) {
                currentBalance = ((Integer) balanceObj).doubleValue();
            }
            
            if (currentBalance < amount) {
                try {
                    throw new Exception("Insufficient wallet balance");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            
            // Calculate new balance
            double updatedBalance = currentBalance - amount;
            
            // Update wallet balance
            transaction.update(db.collection("users").document(userId), "wallet_balance", updatedBalance);
            
            // Create transaction record
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("type", "debit");
            transactionData.put("description", "Payment for " + carName);
            transactionData.put("amount", amount);
            transactionData.put("timestamp", new Date());
            transactionData.put("status", "completed");
            transactionData.put("payment_method", "wallet");
            if (bookingId != null && !bookingId.isEmpty()) {
                transactionData.put("booking_id", bookingId);
            }
            
            transaction.set(db.collection("users").document(userId)
                    .collection("transactions").document(transactionId), transactionData);
            
            // If this is for a booking, update the booking with payment details
            if (bookingId != null && !bookingId.isEmpty()) {
                Map<String, Object> paymentUpdate = new HashMap<>();
                paymentUpdate.put("payment_method", "wallet");
                paymentUpdate.put("payment_id", transactionId);
                paymentUpdate.put("payment_timestamp", new Date());
                
                if (isAdvancePayment) {
                    paymentUpdate.put("advance_payment_done", true);
                    paymentUpdate.put("advance_payment_amount", amount);
                    paymentUpdate.put("remaining_payment", totalPrice - amount);
                } else {
                    paymentUpdate.put("full_payment_done", true);
                    paymentUpdate.put("payment_amount", amount);
                }
                
                transaction.update(db.collection("bookings").document(bookingId), paymentUpdate);
            }
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            // Show success dialog
            new AlertDialog.Builder(this)
                    .setTitle("Payment Successful")
                    .setMessage("Payment of ₹" + amount + " has been successfully processed from your wallet.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Finish activity and return to previous screen
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }).addOnFailureListener(e -> {
            // Show error message
            Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error processing wallet payment", e);
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    // Razorpay payment success callback
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        Log.d(TAG, "Payment successful: " + razorpayPaymentId);
        
        // Store payment in Firestore
        storeRazorpayPaymentInFirestore(razorpayPaymentId);
    }
    
    // Razorpay payment error callback
    @Override
    public void onPaymentError(int code, String description) {
        Log.e(TAG, "Payment failed: " + description + " (code: " + code + ")");
        
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
                errorMessage = "Payment was canceled by user.";
                break;
            case Checkout.TLS_ERROR:
                errorMessage = "TLS connection error. Please update your device.";
                break;
            default:
                errorMessage = "Payment failed: " + description;
                break;
        }
        
        // Show more descriptive error message
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
    
    private void storeRazorpayPaymentInFirestore(String razorpayPaymentId) {
        // Create a payment record in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        
        // Create payment data
        HashMap<String, Object> payment = new HashMap<>();
        payment.put("razorpay_payment_id", razorpayPaymentId);
        payment.put("user_id", userId);
        payment.put("amount", amount);
        payment.put("car_name", carName);
        payment.put("payment_date", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
        payment.put("payment_type", isAdvancePayment ? "advance_payment" : "full_payment");
        payment.put("payment_method", "razorpay");
        payment.put("payment_status", "success");
        
        if (isAdvancePayment) {
            payment.put("total_price", totalPrice);
            payment.put("remaining_amount", totalPrice - amount);
        }
        
        // If this is for a booking, add the booking ID
        if (bookingId != null && !bookingId.isEmpty()) {
            payment.put("booking_id", bookingId);
        }
        
        // Save payment to Firestore
        db.collection("payments")
                .document(razorpayPaymentId)
                .set(payment)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment record saved to Firestore successfully");
                    
                    // If this is for a booking, update the booking with payment details
                    if (bookingId != null && !bookingId.isEmpty()) {
                        updateBookingWithRazorpayDetails(bookingId, razorpayPaymentId);
                    } else {
                        finishRazorpayPaymentProcess(razorpayPaymentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save payment record to Firestore", e);
                    
                    // Show error toast
                    Toast.makeText(this, "Payment successful but failed to save details: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    
                    // Return result anyway
                    finishRazorpayPaymentProcess(razorpayPaymentId);
                });
    }
    
    private void updateBookingWithRazorpayDetails(String bookingId, String paymentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Map<String, Object> paymentUpdate = new HashMap<>();
        paymentUpdate.put("payment_method", "razorpay");
        paymentUpdate.put("payment_id", paymentId);
        paymentUpdate.put("payment_timestamp", new Date());
        
        if (isAdvancePayment) {
            paymentUpdate.put("advance_payment_done", true);
            paymentUpdate.put("advance_payment_amount", amount);
            paymentUpdate.put("remaining_payment", totalPrice - amount);
        } else {
            paymentUpdate.put("full_payment_done", true);
            paymentUpdate.put("payment_amount", amount);
        }
        
        db.collection("bookings").document(bookingId)
                .update(paymentUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking updated with payment details successfully");
                    finishRazorpayPaymentProcess(paymentId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update booking with payment details", e);
                    finishRazorpayPaymentProcess(paymentId);
                });
    }
    
    private void finishRazorpayPaymentProcess(String paymentId) {
        // Show success dialog
        new AlertDialog.Builder(this)
                .setTitle("Payment Successful")
                .setMessage("Your payment of ₹" + amount + " was successful.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Return result to calling activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("payment_id", paymentId);
                    resultIntent.putExtra("payment_method", "razorpay");
                    setResult(RESULT_OK, resultIntent);
                    
                    // Close the activity
                    finish();
                })
                .setCancelable(false)
                .show();
    }
} 