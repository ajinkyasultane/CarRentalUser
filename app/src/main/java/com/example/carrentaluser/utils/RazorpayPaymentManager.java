package com.example.carrentaluser.utils;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

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

/**
 * Manager class for Razorpay payment gateway integration
 */
public class RazorpayPaymentManager {
    private static final String TAG = "RazorpayPaymentManager";
    
    // Razorpay API key - Replace with your actual key from Razorpay dashboard
    // Test mode key is used here
    private static final String RAZORPAY_API_KEY = "rzp_test_Jrr3S8Z52c8foR";
    
    /**
     * Initialize Razorpay SDK
     * Call this method in your Application class or main activity
     */
    public static void initialize() {
        Checkout.preload(com.example.carrentaluser.CarRentalApp.getInstance());
    }
    
    /**
     * Start Razorpay payment process
     *
     * @param activity Activity to launch payment from
     * @param amount Amount in smallest currency unit (paise for INR - so Rs. 100 = 10000 paise)
     * @param carName Car name for payment description
     * @param isAdvancePayment Whether this is an advance payment
     * @param email User email for receipt
     * @param phoneNumber User phone number (optional, can be null)
     * @param callback Callback for payment result
     */
    public static void startPayment(Activity activity, int amount, String carName, 
                                    boolean isAdvancePayment, String email, 
                                    String phoneNumber, RazorpayPaymentCallback callback) {
        try {
            Checkout checkout = new Checkout();
            checkout.setKeyID(RAZORPAY_API_KEY);
            
            // Set image (optional)
            // checkout.setImage(R.drawable.your_logo);
            
            // Create payment options object
            JSONObject options = new JSONObject();
            
            // Payment amount (in paise for INR)
            options.put("amount", amount * 100); // Convert to paise
            options.put("currency", "INR");
            
            // Order information
            String description = isAdvancePayment ? 
                    "50% Advance Payment for " + carName : 
                    "Payment for " + carName;
            options.put("description", description);
            
            // Prefill customer information
            JSONObject prefill = new JSONObject();
            prefill.put("email", email);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                prefill.put("contact", phoneNumber);
            }
            options.put("prefill", prefill);
            
            // Theme customization
            JSONObject theme = new JSONObject();
            theme.put("color", "#3F51B5"); // Primary color of your app
            options.put("theme", theme);
            
            // Start payment
            checkout.open(activity, options);
            
            // Payment result will be received in the implementing activity 
            // which should implement PaymentResultListener
            
            // Store the callback for later use when payment completes
            saveCallback(activity, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting Razorpay payment", e);
            Toast.makeText(activity, "Error starting payment: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            
            if (callback != null) {
                callback.onPaymentError("Payment initialization failed: " + e.getMessage());
            }
        }
    }
    
    // Map to store callbacks based on activity hash
    private static final Map<Integer, RazorpayPaymentCallback> callbackMap = new HashMap<>();
    
    /**
     * Save callback for an activity
     */
    private static void saveCallback(Activity activity, RazorpayPaymentCallback callback) {
        if (activity != null && callback != null) {
            callbackMap.put(activity.hashCode(), callback);
        }
    }
    
    /**
     * Get callback for an activity
     */
    public static RazorpayPaymentCallback getCallback(Activity activity) {
        if (activity != null) {
            return callbackMap.get(activity.hashCode());
        }
        return null;
    }
    
    /**
     * Remove callback when no longer needed
     */
    public static void removeCallback(Activity activity) {
        if (activity != null) {
            callbackMap.remove(activity.hashCode());
        }
    }
    
    /**
     * Handle successful payment and save payment details to Firestore
     *
     * @param activity Activity where payment was made
     * @param paymentData Payment data from Razorpay
     * @param isAdvancePayment Whether this is an advance payment
     * @param carName Car name
     * @param amount Payment amount
     * @param totalPrice Total price (for advance payments)
     */
    public static void handleSuccessfulPayment(Activity activity, PaymentData paymentData,
                                              boolean isAdvancePayment, String carName,
                                              int amount, int totalPrice) {
        // Create a payment record in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Extract payment details
        String paymentId = paymentData.getPaymentId();
        String orderId = paymentData.getOrderId();
        String signature = paymentData.getSignature();
        
        // Create payment data
        HashMap<String, Object> payment = new HashMap<>();
        payment.put("razorpay_payment_id", paymentId);
        payment.put("razorpay_order_id", orderId);
        payment.put("razorpay_signature", signature);
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
        
        // Save payment to Firestore
        db.collection("payments")
                .document(paymentId)
                .set(payment)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment record saved to Firestore successfully");
                    Toast.makeText(activity, "Payment successful!", Toast.LENGTH_SHORT).show();
                    
                    // Notify the callback
                    RazorpayPaymentCallback callback = getCallback(activity);
                    if (callback != null) {
                        callback.onPaymentSuccess(paymentId);
                    }
                    
                    // Remove the callback
                    removeCallback(activity);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save payment record to Firestore", e);
                    
                    // Notify the callback about the Firestore error, but payment was still successful
                    RazorpayPaymentCallback callback = getCallback(activity);
                    if (callback != null) {
                        callback.onPaymentSuccess(paymentId);
                    }
                    
                    // Remove the callback
                    removeCallback(activity);
                });
    }
    
    /**
     * Callback for Razorpay payment
     */
    public interface RazorpayPaymentCallback {
        void onPaymentSuccess(String razorpayPaymentId);
        void onPaymentError(String errorMessage);
    }
} 