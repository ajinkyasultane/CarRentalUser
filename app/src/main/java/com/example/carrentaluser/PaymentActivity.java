package com.example.carrentaluser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.razorpay.Checkout;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.widget.ImageView;

public class PaymentActivity extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = "PaymentActivity";
    
    // Razorpay API key - Replace with your actual key from Razorpay dashboard
    private static final String RAZORPAY_API_KEY = "rzp_test_Jrr3S8Z52c8foR";

    private TextView tvCarName, tvPaymentAmount, tvPaymentDescription;
    private ImageView ivCarImage;
    private Button btnConfirmPayment, btnChooseUpiApp;
    private RadioGroup paymentMethodGroup;
    private RadioButton radioUpi, radioCard;
    private TextInputLayout upiIdLayout;
    private TextInputEditText etUpiId;
    
    private boolean isAdvancePayment = false;
    private int totalPrice = 0;
    private String carName;
    private String carImage;
    private int amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize Razorpay
        Checkout.preload(getApplicationContext());

        // Initialize views
        tvCarName = findViewById(R.id.tv_car_name);
        tvPaymentAmount = findViewById(R.id.tv_payment_amount);
        tvPaymentDescription = findViewById(R.id.tv_payment_description);
        ivCarImage = findViewById(R.id.iv_car_image);
        btnConfirmPayment = findViewById(R.id.btn_confirm_payment);
        paymentMethodGroup = findViewById(R.id.payment_method_group);
        radioUpi = findViewById(R.id.radio_upi);
        radioCard = findViewById(R.id.radio_card);
        upiIdLayout = findViewById(R.id.upi_id_layout);
        etUpiId = findViewById(R.id.et_upi_id);
        btnChooseUpiApp = findViewById(R.id.btn_choose_upi_app);

        // Get intent data
        carName = getIntent().getStringExtra("car_name");
        carImage = getIntent().getStringExtra("car_image");
        amount = getIntent().getIntExtra("amount", 0);
        isAdvancePayment = getIntent().getBooleanExtra("is_advance_payment", false);
        
        if (isAdvancePayment) {
            totalPrice = getIntent().getIntExtra("total_price", 0);
        }

        // Set up views
        tvCarName.setText(carName);
        tvPaymentAmount.setText("₹" + amount);
        
        // Update UI for Razorpay integration
        upiIdLayout.setVisibility(View.GONE); // Not needed for Razorpay
        btnChooseUpiApp.setVisibility(View.GONE); // Not needed for Razorpay
        
        // Change button text to reflect Razorpay
        btnConfirmPayment.setText(isAdvancePayment ? 
                "Pay 50% Advance (₹" + amount + ") with Razorpay" : 
                "Pay with Razorpay");
        
        // Load car image
        if (carImage != null && !carImage.isEmpty()) {
            Glide.with(this)
                    .load(carImage)
                    .placeholder(R.drawable.placeholder_car1)
                    .error(R.drawable.placeholder_car1)
                    .into(ivCarImage);
        }
        
        // Set description based on payment type
        if (isAdvancePayment) {
            tvPaymentDescription.setText("This is a 50% advance payment required for booking with driver. " +
                    "Remaining payment (₹" + (totalPrice - amount) + ") will be collected upon service delivery.");
        } else {
            tvPaymentDescription.setText("Please confirm payment to complete your booking.");
        }
        
        // Update radio button text
        radioUpi.setText("UPI / Wallet / NetBanking (Razorpay)");
        radioCard.setText("Credit/Debit Card (Razorpay)");
        
        // Set up confirm payment button
        btnConfirmPayment.setOnClickListener(v -> {
            startRazorpayPayment();
        });
    }
    
    private void startRazorpayPayment() {
        /* Skip internet connection check as it's causing issues
        if (!CarRentalApp.getInstance().isInternetAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network and try again.", 
                    Toast.LENGTH_LONG).show();
            
            new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("Razorpay requires an active internet connection. Please check your network settings and try again.")
                    .setPositiveButton("OK", null)
                    .show();
            
            // Re-enable the button
            btnConfirmPayment.setEnabled(true);
            return;
        }
        */
    
        // Initialize Razorpay checkout
        Checkout checkout = new Checkout();
        
        // Explicitly log the API key being used (test key)
        Log.d(TAG, "Using Razorpay Test API Key: " + RAZORPAY_API_KEY);
        
        checkout.setKeyID(RAZORPAY_API_KEY);
        
        try {
            // Create options JSON object
            JSONObject options = new JSONObject();
            
            // Validate amount (Razorpay requires minimum of ₹1)
            if (amount < 1) {
                Toast.makeText(this, "Minimum payment amount is ₹1", Toast.LENGTH_SHORT).show();
                // Re-enable the button
                btnConfirmPayment.setEnabled(true);
                return;
            }
            
            // Set amount in paise (100 paise = 1 INR)
            options.put("amount", amount * 100);
            options.put("currency", "INR");
            
            // Add a name for the order (required by Razorpay)
            options.put("name", "Car Rental");
            
            // Set order details
            String description = isAdvancePayment ? 
                    "50% Advance Payment for " + carName : 
                    "Payment for " + carName;
            options.put("description", description);
            
            // Prefill customer information if available
            JSONObject prefill = new JSONObject();
            
            // Get user email from Firebase Auth
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String email = auth.getCurrentUser().getEmail();
                if (email != null && !email.isEmpty()) {
                    prefill.put("email", email);
                    
                    // Add log for debugging
                    Log.d(TAG, "Using email for payment: " + email);
                }
            }
            
            options.put("prefill", prefill);
            
            // Theme customization
            JSONObject theme = new JSONObject();
            theme.put("color", "#4CAF50"); // Green color
            options.put("theme", theme);
            
            // Log payment options for debugging
            Log.d(TAG, "Razorpay payment options: " + options.toString());
            
            // Ensure amount is correct
            Log.d(TAG, "Payment amount in rupees: " + amount);
            Log.d(TAG, "Payment amount in paise: " + (amount * 100));
            
            // Show toast with payment details
            Toast.makeText(this, 
                "Initiating payment of ₹" + amount + 
                (isAdvancePayment ? " (50% advance)" : ""), 
                Toast.LENGTH_SHORT).show();
            
            // Show dialog with test card info before proceeding
            new AlertDialog.Builder(this)
                    .setTitle("Test Payment Information")
                    .setMessage("You are about to make a test payment with Razorpay TEST MODE.\n\n" +
                            "Use these test card details:\n" +
                            "Card Number: 4111 1111 1111 1111\n" +
                            "Expiry: Any future date (e.g. 12/25)\n" +
                            "CVV: Any 3 digits (e.g. 123)\n" +
                            "Name: Any name\n\n" +
                            "For OTP: Use 1234 (or any number)\n\n" +
                            "Continue to Razorpay?")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        // Start Razorpay checkout
                        checkout.open(this, options);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Re-enable the payment button
                        btnConfirmPayment.setEnabled(true);
                    })
                    .show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting Razorpay payment", e);
            Toast.makeText(this, "Error starting payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Re-enable the button
            btnConfirmPayment.setEnabled(true);
        }
    }
    
    // Razorpay payment success callback
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        Log.d(TAG, "Payment successful: " + razorpayPaymentId);
        
        // Get booking ID from intent if available
        String bookingId = getIntent().getStringExtra("booking_id");
        
        // Store payment in Firestore
        storePaymentInFirestore(razorpayPaymentId, bookingId);
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
        
        // Re-enable the payment button
        btnConfirmPayment.setEnabled(true);
    }
    
    private void storePaymentInFirestore(String razorpayPaymentId, String bookingId) {
        // Create a payment record in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
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
                        updateBookingWithPaymentDetails(bookingId, razorpayPaymentId);
                    } else {
                        finishPaymentProcess(razorpayPaymentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save payment record to Firestore", e);
                    
                    // Show error toast
                    Toast.makeText(this, "Payment successful but failed to save details: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    
                    // Return result anyway
                    finishPaymentProcess(razorpayPaymentId);
                });
    }
    
    private void updateBookingWithPaymentDetails(String bookingId, String paymentId) {
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
                    finishPaymentProcess(paymentId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update booking with payment details", e);
                    finishPaymentProcess(paymentId);
                });
    }
    
    private void finishPaymentProcess(String paymentId) {
        // Show success toast
        Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
        
        // Return result to calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("payment_id", paymentId);
        resultIntent.putExtra("payment_method", "razorpay");
        setResult(Activity.RESULT_OK, resultIntent);
        
        // Close the activity
        finish();
    }
} 