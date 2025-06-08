package com.example.carrentaluser.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Manager class for payment gateway integration.
 * This will be used in the future to integrate with payment gateways.
 */
public class PaymentGatewayManager {
    
    // Payment methods
    public static final String PAYMENT_METHOD_UPI = "upi";
    public static final String PAYMENT_METHOD_CARD = "card";
    public static final String PAYMENT_METHOD_NET_BANKING = "net_banking";
    public static final String PAYMENT_METHOD_WALLET = "wallet";
    
    // Payment status
    public static final String PAYMENT_STATUS_SUCCESS = "success";
    public static final String PAYMENT_STATUS_FAILED = "failed";
    public static final String PAYMENT_STATUS_PENDING = "pending";
    
    // Payment types
    public static final String PAYMENT_TYPE_ADVANCE = "advance_payment";
    public static final String PAYMENT_TYPE_FULL = "full_payment";
    public static final String PAYMENT_TYPE_REMAINING = "remaining_payment";
    
    /**
     * Process a payment and store the result in Firestore
     * 
     * @param context Application context
     * @param paymentDetails Payment details
     * @param listener Payment result listener
     */
    public static void processPayment(Context context, Map<String, Object> paymentDetails, 
                                     PaymentResultListener listener) {
        // In a real implementation, this would integrate with a payment gateway
        // For now, we'll simulate a successful payment
        
        // Generate a payment ID
        String paymentId = UUID.randomUUID().toString();
        
        // Add payment ID to details
        paymentDetails.put("payment_id", paymentId);
        
        // Add timestamp
        paymentDetails.put("payment_date", 
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
        
        // Add user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        paymentDetails.put("user_id", userId);
        
        // Save to Firestore
        FirebaseFirestore.getInstance()
                .collection("payments")
                .document(paymentId)
                .set(paymentDetails)
                .addOnSuccessListener(aVoid -> {
                    // Payment record created successfully
                    if (listener != null) {
                        HashMap<String, Object> result = new HashMap<>();
                        result.put("payment_id", paymentId);
                        result.put("status", PAYMENT_STATUS_SUCCESS);
                        listener.onPaymentSuccess(result);
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to create payment record
                    if (listener != null) {
                        HashMap<String, Object> result = new HashMap<>();
                        result.put("error", e.getMessage());
                        result.put("status", PAYMENT_STATUS_FAILED);
                        listener.onPaymentFailure(result);
                    }
                });
    }
    
    /**
     * Handle UPI payment with specified UPI app
     * 
     * @param activity Activity to launch UPI payment from
     * @param upiId UPI ID to send payment to
     * @param amount Payment amount
     * @param description Payment description
     * @param upiAppPackage UPI app package name
     * @param requestCode Request code for activity result
     * @return true if UPI app was launched, false otherwise
     */
    public static boolean payWithUpi(Activity activity, String upiId, String amount, 
                                    String description, String upiAppPackage, int requestCode) {
        // Check if UPI payment is possible
        if (!UpiPaymentHelper.isUpiPaymentPossible(activity)) {
            Toast.makeText(activity, "UPI payment is not available on this device", 
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Create UPI payment intent
        Intent upiPaymentIntent = UpiPaymentHelper.createUpiPaymentIntent(
                upiId, "Car Rental", description, amount, upiAppPackage);
        
        // Check if intent can be resolved
        if (upiPaymentIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(upiPaymentIntent, requestCode);
            return true;
        } else {
            Toast.makeText(activity, "No UPI app found to handle this payment", 
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Show a dialog to select a UPI app
     * 
     * @param context Context to show dialog
     * @param upiId UPI ID to pay to
     * @param amount Payment amount
     * @param description Payment description
     * @param callback Callback when UPI app is selected
     */
    public static void showUpiAppSelectionDialog(Context context, String upiId, String amount,
                                                String description, UpiAppSelectionCallback callback) {
        // Get available UPI apps
        if (!UpiPaymentHelper.isUpiPaymentPossible(context)) {
            Toast.makeText(context, "UPI payment is not available on this device", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // In a real implementation, you would:
        // 1. Get the list of available UPI apps
        // 2. Show them in a dialog
        // 3. When an app is selected, launch it
        
        // For now, we'll show a placeholder dialog
        new AlertDialog.Builder(context)
            .setTitle("Select UPI App")
            .setMessage("In the future version, this will show a list of UPI apps installed on your device.")
            .setPositiveButton("Simulate Payment", (dialog, which) -> {
                if (callback != null) {
                    callback.onUpiAppSelected("dummy_package", "Simulated UPI App");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Interface for payment result callbacks
     */
    public interface PaymentResultListener {
        void onPaymentSuccess(Map<String, Object> result);
        void onPaymentFailure(Map<String, Object> error);
    }
    
    /**
     * Interface for UPI app selection callbacks
     */
    public interface UpiAppSelectionCallback {
        void onUpiAppSelected(String packageName, String appName);
    }
} 