package com.example.carrentaluser;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import com.google.firebase.firestore.FirebaseFirestore;
import android.app.AlertDialog;
import java.util.Date;
import java.util.HashMap;

/**
 * This is a test activity to demonstrate Razorpay integration.
 * You can use this activity to test Razorpay payments directly.
 */
public class RazorpayTestActivity extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = "RazorpayTestActivity";
    private static final String RAZORPAY_API_KEY = "rzp_test_Jrr3S8Z52c8foR"; // Replace with your test key
    
    private EditText etAmount, etEmail, etPhone, etDescription;
    private Button btnPay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_razorpay_test);
        
        // Initialize Razorpay
        Checkout.preload(getApplicationContext());
        
        // Initialize views
        etAmount = findViewById(R.id.et_amount);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDescription = findViewById(R.id.et_description);
        btnPay = findViewById(R.id.btn_pay);
        
        // Set up pay button
        btnPay.setOnClickListener(v -> {
            // Validate input
            if (validateInput()) {
                startRazorpayPayment();
            }
        });
    }
    
    private boolean validateInput() {
        String amount = etAmount.getText().toString().trim();
        if (amount.isEmpty()) {
            etAmount.setError("Please enter amount");
            return false;
        }
        
        try {
            // Check if amount is a valid number
            double amountValue = Double.parseDouble(amount);
            if (amountValue <= 0) {
                etAmount.setError("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount format");
            return false;
        }
        
        return true;
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
            
            return;
        }
        */
        
        // Get input values
        String amountStr = etAmount.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        
        // Validate the amount (minimum 1 INR for Razorpay)
        double amountInRupees = Double.parseDouble(amountStr);
        if (amountInRupees < 1.0) {
            etAmount.setError("Minimum amount is ₹1");
            Toast.makeText(this, "Razorpay requires minimum amount of ₹1", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Convert amount to paise (1 INR = 100 paise)
        int amountInPaise = (int) (amountInRupees * 100);
        
        // Initialize Razorpay checkout
        Checkout checkout = new Checkout();
        
        // Explicitly log the API key being used (test key)
        Log.d(TAG, "Using Razorpay Test API Key: " + RAZORPAY_API_KEY);
        
        checkout.setKeyID(RAZORPAY_API_KEY);
        
        try {
            // Create options JSON object
            JSONObject options = new JSONObject();
            
            // Set amount in paise
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            
            // Add a name for the order (required by Razorpay)
            options.put("name", "Car Rental");
            
            // Set description if provided
            if (!description.isEmpty()) {
                options.put("description", description);
            } else {
                options.put("description", "Test Payment");
            }
            
            // Prefill customer information if available
            JSONObject prefill = new JSONObject();
            if (!email.isEmpty()) {
                prefill.put("email", email);
                Log.d(TAG, "Adding email to payment: " + email);
            }
            if (!phone.isEmpty()) {
                prefill.put("contact", phone);
                Log.d(TAG, "Adding phone to payment: " + phone);
            }
            options.put("prefill", prefill);
            
            // Theme customization
            JSONObject theme = new JSONObject();
            theme.put("color", "#4CAF50"); // Green color
            options.put("theme", theme);
            
            // Log full options for debugging
            Log.d(TAG, "Payment options: " + options.toString());
            Log.d(TAG, "Amount in rupees: " + amountInRupees + ", Amount in paise: " + amountInPaise);
            
            // Show a toast with payment info
            Toast.makeText(this, "Starting payment of ₹" + amountInRupees, Toast.LENGTH_SHORT).show();
            
            // Display a dialog with test card details
            new AlertDialog.Builder(this)
                    .setTitle("Test Payment Information")
                    .setMessage("IMPORTANT: This is a TEST MODE payment.\n\n" +
                            "Use these test card details:\n" +
                            "Card Number: 4111 1111 1111 1111\n" +
                            "Expiry: Any future date (e.g. 12/25)\n" +
                            "CVV: Any 3 digits (e.g. 123)\n" +
                            "Name: Any name\n\n" +
                            "For OTP: Enter 1234 (or any number)\n\n" +
                            "Continue to Razorpay?")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        // Start Razorpay checkout
                        checkout.open(this, options);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting Razorpay payment", e);
            Toast.makeText(this, "Error starting payment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    // Razorpay payment success callback
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        Log.d(TAG, "Payment successful: " + razorpayPaymentId);
        
        // Store in Firebase for record keeping
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            HashMap<String, Object> payment = new HashMap<>();
            payment.put("payment_id", razorpayPaymentId);
            payment.put("amount", etAmount.getText().toString().trim());
            payment.put("timestamp", new Date());
            payment.put("status", "success");
            payment.put("type", "test_payment");
            
            db.collection("test_payments")
                    .document(razorpayPaymentId)
                    .set(payment)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Test payment record saved successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save test payment record", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error saving test payment", e);
        }
        
        Toast.makeText(this, "Payment Successful: " + razorpayPaymentId, Toast.LENGTH_LONG).show();
        
        // Show payment success dialog with details
        new AlertDialog.Builder(this)
                .setTitle("Payment Successful")
                .setMessage("Payment ID: " + razorpayPaymentId + "\n\nAmount: ₹" + etAmount.getText().toString().trim())
                .setPositiveButton("OK", null)
                .show();
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
                errorMessage = "Payment was canceled by user.";
                break;
            case Checkout.TLS_ERROR:
                errorMessage = "TLS connection error. Please update your device.";
                break;
            default:
                errorMessage = "Payment Failed: " + description + " (Code: " + code + ")";
                break;
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        
        // Show error dialog with more details
        new AlertDialog.Builder(this)
                .setTitle("Payment Failed")
                .setMessage("Error: " + description + "\nError Code: " + code + 
                        "\n\nPlease ensure you're using test card details for test mode:\n" +
                        "Card: 4111 1111 1111 1111\nExpiry: Any future date\nCVV: Any 3 digits")
                .setPositiveButton("OK", null)
                .show();
    }
} 